package br.com.fenix.bilingualreader.service.translate

import android.content.Context
import br.com.fenix.bilingualreader.model.enums.Languages
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitTranslator(private val context: Context) {

    private val mLOGGER = LoggerFactory.getLogger(MlKitTranslator::class.java)
    private val translators = ConcurrentHashMap<String, Translator>()
    private val languageIdentifier: LanguageIdentifier = LanguageIdentification.getClient()

    fun toTranslateLanguage(language: Languages): String? = when (language) {
        Languages.PORTUGUESE, Languages.PORTUGUESE_GOOGLE -> TranslateLanguage.PORTUGUESE
        Languages.ENGLISH -> TranslateLanguage.ENGLISH
        Languages.JAPANESE -> TranslateLanguage.JAPANESE
    }

    fun fromBcp47(code: String?): Languages? {
        if (code.isNullOrBlank() || code == LanguageIdentifier.UNDETERMINED_LANGUAGE_TAG) return null
        return when (code.lowercase().take(2)) {
            "pt" -> Languages.PORTUGUESE
            "en" -> Languages.ENGLISH
            "ja" -> Languages.JAPANESE
            else -> null
        }
    }

    suspend fun detectLanguage(text: String): Languages? {
        if (text.isBlank()) return null
        return suspendCancellableCoroutine { cont ->
            languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { code ->
                    if (cont.isActive) cont.resume(fromBcp47(code))
                }
                .addOnFailureListener { e ->
                    mLOGGER.warn("Language ID failed: ${e.message}")
                    if (cont.isActive) cont.resume(null)
                }
        }
    }

    suspend fun ensureModel(from: Languages, to: Languages) {
        val source = toTranslateLanguage(from) ?: return
        val target = toTranslateLanguage(to) ?: return
        if (source == target) return
        getTranslator(source, target)
    }

    suspend fun translate(text: String, from: Languages, to: Languages): String {
        if (text.isBlank() || from == to) return text
        if ((from == Languages.PORTUGUESE || from == Languages.PORTUGUESE_GOOGLE) &&
            (to == Languages.PORTUGUESE || to == Languages.PORTUGUESE_GOOGLE)
        ) return text

        val source = toTranslateLanguage(from) ?: return text
        val target = toTranslateLanguage(to) ?: return text
        if (source == target) return text

        val translator = getTranslator(source, target)
        return suspendCancellableCoroutine { cont ->
            translator.translate(text)
                .addOnSuccessListener { translated ->
                    if (cont.isActive) cont.resume(translated)
                }
                .addOnFailureListener { e ->
                    mLOGGER.error("Translate failed: ${e.message}", e)
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }
    }

    suspend fun translateIfNeeded(
        text: String,
        hintSource: Languages?,
        userLanguage: Languages
    ): Pair<String, Languages?> {
        val detected = detectLanguage(text) ?: hintSource
        if (detected == null || sameLanguage(detected, userLanguage)) {
            return text to detected
        }
        return try {
            ensureModel(detected, userLanguage)
            translate(text, detected, userLanguage) to detected
        } catch (e: Exception) {
            mLOGGER.warn("translateIfNeeded fallback to original: ${e.message}")
            text to detected
        }
    }

    fun sameLanguage(a: Languages, b: Languages): Boolean {
        fun normalize(l: Languages) = when (l) {
            Languages.PORTUGUESE_GOOGLE -> Languages.PORTUGUESE
            else -> l
        }
        return normalize(a) == normalize(b)
    }

    private suspend fun getTranslator(source: String, target: String): Translator {
        val key = "$source->$target"
        translators[key]?.let { return it }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
        val translator = Translation.getClient(options)
        suspendCancellableCoroutine { cont ->
            val conditions = DownloadConditions.Builder().build()
            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    if (cont.isActive) cont.resume(Unit)
                }
                .addOnFailureListener { e ->
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }
        translators[key] = translator
        return translator
    }

    fun close() {
        translators.values.forEach {
            try {
                it.close()
            } catch (_: Exception) {
            }
        }
        translators.clear()
        languageIdentifier.close()
    }

    companion object {
        @Volatile
        private var INSTANCE: MlKitTranslator? = null

        fun getInstance(context: Context): MlKitTranslator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MlKitTranslator(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
