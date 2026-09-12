package br.com.fenix.bilingualreader.service.llm

import android.content.Context
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.LlmUse
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.llm.openrouter.OpenRouterClient
import br.com.fenix.bilingualreader.service.llm.openrouter.OpenRouterImagePart
import br.com.fenix.bilingualreader.service.llm.openrouter.OpenRouterImagePartUrl
import br.com.fenix.bilingualreader.service.llm.openrouter.OpenRouterMessage
import br.com.fenix.bilingualreader.service.llm.openrouter.OpenRouterTextPart
import br.com.fenix.bilingualreader.util.helpers.LlmSettings
import kotlinx.coroutines.flow.Flow

class OpenRouterLlmBackend(
    private val context: Context,
    private val use: LlmUse = LlmUse.QA,
    private val type: Type = Type.BOOK,
    private val customModel: String? = null
) : LlmBackend {

    override val isCloud: Boolean = true

    private val client = OpenRouterClient(context)

    override suspend fun ensureReady() {
        if (LlmSettings.resolveApiKey(context).isBlank()) {
            throw IllegalStateException(context.getString(R.string.llm_error_openrouter_key_missing))
        }
    }

    override fun generateStreaming(request: LlmChatRequest): Flow<Pair<String, Boolean>> {
        val apiKey = LlmSettings.resolveApiKey(context)
        if (apiKey.isBlank()) {
            throw IllegalStateException(context.getString(R.string.llm_error_openrouter_key_missing))
        }
        val model = customModel?.ifBlank { null } ?: LlmSettings.openRouterModelFor(context, use, type)
        val temperature = LlmSettings.temperature(context)
        val messages = mutableListOf<OpenRouterMessage>()
        messages.add(OpenRouterMessage(role = "system", content = request.system))
        for (h in request.history) {
            val role = if (h.role.equals("user", ignoreCase = true)) "user" else "assistant"
            messages.add(OpenRouterMessage(role = role, content = h.text))
        }

        if (request.imagesBase64.isNotEmpty()) {
            val parts = mutableListOf<Any>()
            parts.add(OpenRouterTextPart(text = request.user))
            for (img in request.imagesBase64) {
                parts.add(OpenRouterImagePart(image_url = OpenRouterImagePartUrl("data:image/jpeg;base64,$img")))
            }
            messages.add(OpenRouterMessage(role = "user", content = parts))
        } else {
            messages.add(OpenRouterMessage(role = "user", content = request.user))
        }

        return client.streamChat(apiKey, model, messages, temperature)
    }
}
