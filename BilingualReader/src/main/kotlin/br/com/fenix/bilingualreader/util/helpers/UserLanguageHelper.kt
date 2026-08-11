package br.com.fenix.bilingualreader.util.helpers

import android.content.Context
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import java.util.Locale

object UserLanguageHelper {

    fun getUserLanguage(context: Context): Languages {
        val prefs = GeneralConsts.getSharedPreferences(context)
        val stored = prefs.getString(GeneralConsts.KEYS.SUBTITLE.TRANSLATE, null)
        if (!stored.isNullOrBlank()) {
            return try {
                Languages.valueOf(stored)
            } catch (_: Exception) {
                fromLocale(Locale.getDefault())
            }
        }
        return fromLocale(Locale.getDefault())
    }

    fun fromLocale(locale: Locale): Languages {
        return when (locale.language.lowercase(Locale.ROOT)) {
            "pt" -> Languages.PORTUGUESE
            "ja" -> Languages.JAPANESE
            else -> Languages.ENGLISH
        }
    }

    fun maxContextChars(context: Context): Int {
        return LlmSettings.maxContextChars(context)
    }

    fun isLlmEnabled(context: Context): Boolean {
        return GeneralConsts.getSharedPreferences(context)
            .getBoolean(GeneralConsts.KEYS.LLM.ENABLED, true)
    }
}
