package br.com.fenix.bilingualreader.service.llm

import android.content.Context
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.LlmUse
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.llm.openrouter.OpenRouterClient
import br.com.fenix.bilingualreader.service.llm.openrouter.OpenRouterMessage
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
        val messages = listOf(
            OpenRouterMessage(role = "system", content = request.system),
            OpenRouterMessage(role = "user", content = request.user)
        )
        return client.streamChat(apiKey, model, messages, temperature)
    }
}
