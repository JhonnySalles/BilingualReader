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
                close(IllegalStateException(context.getString(R.string.llm_error_openrouter_network), e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        close(IllegalStateException(mapHttpError(resp.code)))
                        return
                    }
                    val source = resp.body?.source()
                    if (source == null) {
                        close(IllegalStateException(context.getString(R.string.llm_error_openrouter_network)))
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
                            chunk.error?.message?.let { msg ->
                                close(IllegalStateException(msg))
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
                        close(IllegalStateException(context.getString(R.string.llm_error_openrouter_network), e))
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
                name = FREE_ROUTER_NAME
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
                        OpenRouterModelInfo(
                            id = entry.id!!.trim(),
                            name = entry.name?.trim().orEmpty().ifBlank { entry.id!!.trim() }
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

    private fun mapHttpError(code: Int): String {
        return when (code) {
            401, 403 -> context.getString(R.string.llm_error_openrouter_unauthorized)
            402 -> context.getString(R.string.llm_error_openrouter_payment)
            429 -> context.getString(R.string.llm_error_openrouter_rate_limit)
            else -> context.getString(R.string.llm_error_openrouter_http, code)
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
