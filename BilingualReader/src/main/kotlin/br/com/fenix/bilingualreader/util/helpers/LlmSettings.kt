package br.com.fenix.bilingualreader.util.helpers

import android.content.Context
import br.com.fenix.bilingualreader.model.enums.LlmProvider
import br.com.fenix.bilingualreader.model.enums.LlmUse
import br.com.fenix.bilingualreader.model.enums.Type
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

    fun openRouterModel(context: Context): String = openRouterModelBook(context)

    fun openRouterModelBook(context: Context): String {
        return GeneralConsts.getSharedPreferences(context)
            .getString(
                GeneralConsts.KEYS.LLM.BOOK_OPENROUTER_MODEL,
                GeneralConsts.KEYS.LLM.DEFAULT_OPENROUTER_MODEL
            ) ?: GeneralConsts.KEYS.LLM.DEFAULT_OPENROUTER_MODEL
    }

    fun openRouterModelBookSummary(context: Context): String {
        return GeneralConsts.getSharedPreferences(context)
            .getString(
                GeneralConsts.KEYS.LLM.BOOK_OPENROUTER_MODEL_SUMMARY,
                GeneralConsts.KEYS.LLM.DEFAULT_OPENROUTER_MODEL
            ) ?: GeneralConsts.KEYS.LLM.DEFAULT_OPENROUTER_MODEL
    }

    fun openRouterModelManga(context: Context): String {
        return GeneralConsts.getSharedPreferences(context)
            .getString(
                GeneralConsts.KEYS.LLM.MANGA_OPENROUTER_MODEL,
                GeneralConsts.KEYS.LLM.DEFAULT_OPENROUTER_MODEL
            ) ?: GeneralConsts.KEYS.LLM.DEFAULT_OPENROUTER_MODEL
    }

    fun openRouterModelFor(context: Context, use: LlmUse, type: Type = Type.BOOK): String {
        return when (use) {
            LlmUse.QA -> if (type == Type.BOOK) openRouterModelBook(context) else openRouterModelManga(context)
            LlmUse.SUMMARY -> openRouterModelBookSummary(context)
        }
    }

    fun maxBookChapters(context: Context): Int {
        return GeneralConsts.getSharedPreferences(context)
            .getInt(
                GeneralConsts.KEYS.LLM.MAX_BOOK_CHAPTERS,
                GeneralConsts.KEYS.LLM.DEFAULT_MAX_BOOK_CHAPTERS
            )
            .coerceIn(1, 20)
    }

    fun maxMangaPages(context: Context): Int {
        return GeneralConsts.getSharedPreferences(context)
            .getInt(
                GeneralConsts.KEYS.LLM.MAX_MANGA_PAGES,
                GeneralConsts.KEYS.LLM.DEFAULT_MAX_MANGA_PAGES
            )
            .coerceIn(1, 50)
    }

    fun defaultBookChapters(): Int = GeneralConsts.KEYS.LLM.DEFAULT_BOOK_CHAPTERS

    fun defaultMangaRadius(): Int = GeneralConsts.KEYS.LLM.DEFAULT_MANGA_RADIUS

    /** Temperature in 0f..1f from stored int 0..100. */
    fun temperature(context: Context): Float {
        val stored = GeneralConsts.getSharedPreferences(context)
            .getInt(
                GeneralConsts.KEYS.LLM.TEMPERATURE,
                GeneralConsts.KEYS.LLM.DEFAULT_TEMPERATURE
            )
            .coerceIn(0, 100)
        return stored / 100f
    }

    fun temperaturePercent(context: Context): Int {
        return GeneralConsts.getSharedPreferences(context)
            .getInt(
                GeneralConsts.KEYS.LLM.TEMPERATURE,
                GeneralConsts.KEYS.LLM.DEFAULT_TEMPERATURE
            )
            .coerceIn(0, 100)
    }

    fun selectionPrefsKey(type: Type, referenceId: Long): String =
        "${GeneralConsts.KEYS.LLM.SELECTION_PREFIX}${type.name}_$referenceId"

    fun loadSelection(context: Context, type: Type, referenceId: Long): String? {
        return GeneralConsts.getSharedPreferences(context)
            .getString(selectionPrefsKey(type, referenceId), null)
    }

    fun saveSelection(context: Context, type: Type, referenceId: Long, value: String) {
        GeneralConsts.getSharedPreferences(context).edit()
            .putString(selectionPrefsKey(type, referenceId), value)
            .apply()
    }
}
