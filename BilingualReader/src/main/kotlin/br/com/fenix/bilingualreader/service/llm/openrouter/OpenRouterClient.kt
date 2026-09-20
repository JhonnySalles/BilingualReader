package br.com.fenix.bilingualreader.service.llm.openrouter

import android.content.Context
import br.com.fenix.bilingualreader.R
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenRouterClient(private val context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(OpenRouterClient::class.java)
    private val gson = Gson()
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun streamChat(
        apiKey: String,
        model: String,
        messages: List<OpenRouterMessage>,
        temperature: Float? = null
    ): Flow<Pair<String, Boolean>> = callbackFlow {
        val bodyJson = gson.toJson(
            OpenRouterChatRequest(
                model = model,
                messages = messages,
                stream = true,
                temperature = temperature
            )
        )
        val request = Request.Builder()
            .url(CHAT_URL)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("HTTP-Referer", "https://github.com/JhonnySalles/BilingualReader")
            .header("X-Title", "BilingualReader")
            .post(bodyJson.toRequestBody(JSON))
            .build()

        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                close(br.com.fenix.bilingualreader.service.llm.OpenRouterNetworkException(context.getString(R.string.llm_error_openrouter_network), e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        close(createHttpException(resp.code))
                        return
                    }
                    val source = resp.body?.source()
                    if (source == null) {
                        close(br.com.fenix.bilingualreader.service.llm.OpenRouterNetworkException(context.getString(R.string.llm_error_openrouter_network)))
                        return
                    }

                    val accumulated = StringBuilder()
                    try {
                        val reader = BufferedReader(source.inputStream().reader())
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val raw = line ?: continue
                            if (raw.isBlank() || raw.startsWith(":")) continue
                            if (!raw.startsWith("data:")) continue
                            val payload = raw.removePrefix("data:").trim()
                            if (payload == "[DONE]") {
                                trySend(accumulated.toString() to true)
                                close()
                                return
                            }
                            val chunk = try {
                                gson.fromJson(payload, OpenRouterStreamChunk::class.java)
                            } catch (e: Exception) {
                                mLOGGER.warn("OpenRouter SSE parse skip: ${e.message}")
                                continue
                            }
                            chunk.error?.let { err ->
                                val msg = err.message ?: context.getString(R.string.llm_assistant_error)
                                val code = err.code ?: 0
                                val exception = when (code) {
                                    401, 403 -> br.com.fenix.bilingualreader.service.llm.OpenRouterAuthException(msg)
                                    402 -> br.com.fenix.bilingualreader.service.llm.OpenRouterQuotaExceededException(msg)
                                    429 -> br.com.fenix.bilingualreader.service.llm.OpenRouterRateLimitException(msg)
                                    503 -> br.com.fenix.bilingualreader.service.llm.OpenRouterServiceUnavailableException(msg)
                                    else -> {
                                        if (msg.contains("rate limit", ignoreCase = true) || msg.contains("429")) {
                                            br.com.fenix.bilingualreader.service.llm.OpenRouterRateLimitException(msg)
                                        } else if (msg.contains("credit", ignoreCase = true) || msg.contains("quota", ignoreCase = true) || msg.contains("balance", ignoreCase = true)) {
                                            br.com.fenix.bilingualreader.service.llm.OpenRouterQuotaExceededException(msg)
                                        } else if (msg.contains("key", ignoreCase = true) || msg.contains("auth", ignoreCase = true) || msg.contains("unauthorized", ignoreCase = true)) {
                                            br.com.fenix.bilingualreader.service.llm.OpenRouterAuthException(msg)
                                        } else {
                                            IllegalStateException(msg)
                                        }
                                    }
                                }
                                close(exception)
                                return
                            }
                            val delta = chunk.choices?.firstOrNull()?.delta?.content.orEmpty()
                            if (delta.isNotEmpty()) {
                                accumulated.append(delta)
                                trySend(accumulated.toString() to false)
                            }
                        }
                        trySend(accumulated.toString() to true)
                        close()
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        close(br.com.fenix.bilingualreader.service.llm.OpenRouterNetworkException(context.getString(R.string.llm_error_openrouter_network), e))
                    }
                }
            }
        })

        awaitClose { call.cancel() }
    }.flowOn(Dispatchers.IO)

    suspend fun listFreeModels(): List<OpenRouterModelInfo> = withContext(Dispatchers.IO) {
        val fallback = listOf(
            OpenRouterModelInfo(
                id = FREE_ROUTER_ID,
                name = FREE_ROUTER_NAME,
                hasVision = true
            )
        )
        return@withContext try {
            val request = Request.Builder()
                .url(MODELS_URL)
                .header("Content-Type", "application/json")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    mLOGGER.warn("OpenRouter models HTTP ${response.code}")
                    return@use fallback
                }
                val body = response.body?.string().orEmpty()
                val parsed = gson.fromJson(body, OpenRouterModelsResponse::class.java)
                val free = parsed.data.orEmpty()
                    .asSequence()
                    .filter { entry ->
                        val id = entry.id?.trim().orEmpty()
                        id.isNotEmpty() && isFreePricing(entry.pricing)
                    }
                    .map { entry ->
                        val modality = entry.architecture?.modality.orEmpty().lowercase()
                        val inputs = entry.architecture?.input_modalities.orEmpty().map { it.lowercase() }
                        val hasVision = modality.contains("image") || modality.contains("vision") || inputs.contains("image") || inputs.contains("vision")
                        OpenRouterModelInfo(
                            id = entry.id!!.trim(),
                            name = entry.name?.trim().orEmpty().ifBlank { entry.id!!.trim() },
                            hasVision = hasVision
                        )
                    }
                    .sortedBy { it.name.lowercase() }
                    .toList()

                val withoutRouter = free.filterNot { it.id == FREE_ROUTER_ID }
                fallback + withoutRouter
            }
        } catch (e: Exception) {
            mLOGGER.warn("OpenRouter listFreeModels failed: ${e.message}")
            fallback
        }
    }

    private fun isFreePricing(pricing: OpenRouterPricing?): Boolean {
        if (pricing == null) return false
        val prompt = pricing.prompt?.toDoubleOrNull() ?: return false
        val completion = pricing.completion?.toDoubleOrNull() ?: return false
        return prompt == 0.0 && completion == 0.0
    }

    private fun createHttpException(code: Int): Exception {
        return when (code) {
            401, 403 -> br.com.fenix.bilingualreader.service.llm.OpenRouterAuthException(context.getString(R.string.llm_error_openrouter_unauthorized))
            402 -> br.com.fenix.bilingualreader.service.llm.OpenRouterQuotaExceededException(context.getString(R.string.llm_error_openrouter_payment))
            429 -> br.com.fenix.bilingualreader.service.llm.OpenRouterRateLimitException(context.getString(R.string.llm_error_openrouter_rate_limit))
            503 -> br.com.fenix.bilingualreader.service.llm.OpenRouterServiceUnavailableException(context.getString(R.string.llm_error_openrouter_http, code))
            else -> IllegalStateException(context.getString(R.string.llm_error_openrouter_http, code))
        }
    }

    companion object {
        private const val CHAT_URL = "https://openrouter.ai/api/v1/chat/completions"
        private const val MODELS_URL = "https://openrouter.ai/api/v1/models"
        const val FREE_ROUTER_ID = "openrouter/free"
        const val FREE_ROUTER_NAME = "Free Models Router"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
