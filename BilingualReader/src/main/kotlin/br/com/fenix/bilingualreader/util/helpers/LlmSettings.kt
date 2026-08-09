package br.com.fenix.bilingualreader.util.helpers

import android.content.Context
import br.com.fenix.bilingualreader.model.enums.LlmProvider
import br.com.fenix.bilingualreader.service.llm.LlmInferenceEngine
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.secrets.Secrets

object LlmSettings {

    fun getProvider(context: Context): LlmProvider {
        val value = GeneralConsts.getSharedPreferences(context)
            .getString(
                GeneralConsts.KEYS.LLM.PROVIDER,
                GeneralConsts.KEYS.LLM.DEFAULT_PROVIDER
            )
        return LlmProvider.fromPref(value)
    }

    fun effectiveProvider(context: Context): LlmProvider {
        return when (getProvider(context)) {
            LlmProvider.ON_DEVICE -> LlmProvider.ON_DEVICE
            LlmProvider.OPENROUTER -> LlmProvider.OPENROUTER
            LlmProvider.AUTO -> {
                if (LlmInferenceEngine.isNativeBackendAvailable()) LlmProvider.ON_DEVICE
                else LlmProvider.OPENROUTER
            }
        }
    }

    fun resolveApiKey(context: Context): String {
        val prefsKey = GeneralConsts.getSharedPreferences(context)
            .getString(GeneralConsts.KEYS.LLM.OPENROUTER_API_KEY, null)
            ?.trim()
            .orEmpty()
        if (prefsKey.isNotEmpty()) return prefsKey
        return try {
            Secrets.getSecrets(context).getOpenRouterApiKey().trim()
        } catch (_: Exception) {
            ""
        }
    }

    fun openRouterModel(context: Context): String {
        return GeneralConsts.getSharedPreferences(context)
            .getString(
                GeneralConsts.KEYS.LLM.OPENROUTER_MODEL,
                GeneralConsts.KEYS.LLM.DEFAULT_OPENROUTER_MODEL
            ) ?: GeneralConsts.KEYS.LLM.DEFAULT_OPENROUTER_MODEL
    }
}
