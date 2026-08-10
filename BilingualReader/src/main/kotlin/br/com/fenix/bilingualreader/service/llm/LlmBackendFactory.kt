package br.com.fenix.bilingualreader.service.llm

import android.content.Context
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.LlmProvider
import br.com.fenix.bilingualreader.model.enums.LlmUse
import br.com.fenix.bilingualreader.util.helpers.LlmSettings

object LlmBackendFactory {

    fun resolve(context: Context, use: LlmUse = LlmUse.QA): LlmBackend {
        return when (LlmSettings.effectiveProvider(context)) {
            LlmProvider.OPENROUTER -> OpenRouterLlmBackend(context.applicationContext, use)
            LlmProvider.ON_DEVICE -> OnDeviceLlmBackend(context.applicationContext)
            LlmProvider.AUTO -> error("effectiveProvider must not return AUTO")
        }
    }

    fun requireReadyOrMessage(context: Context): String? {
        val provider = LlmSettings.effectiveProvider(context)
        return when (provider) {
            LlmProvider.OPENROUTER -> {
                if (LlmSettings.resolveApiKey(context).isBlank()) {
                    context.getString(R.string.llm_error_openrouter_key_missing)
                } else null
            }
            LlmProvider.ON_DEVICE -> {
                if (!LlmInferenceEngine.isNativeBackendAvailable()) {
                    context.getString(R.string.llm_error_unsupported_device)
                } else null
            }
            else -> null
        }
    }
}
