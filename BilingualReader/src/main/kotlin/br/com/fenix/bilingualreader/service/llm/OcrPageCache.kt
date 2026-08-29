package br.com.fenix.bilingualreader.service.llm

import android.content.Context
import android.util.LruCache
import br.com.fenix.bilingualreader.model.enums.Languages
import java.io.File

/**
 * Memory + disk cache for manga page OCR (+ translated) text used by [MangaContextProvider].
 */
class OcrPageCache private constructor(private val context: Context) {

    private val memory = LruCache<String, String>(32)
    private val root: File by lazy {
        File(context.filesDir, "llm/ocr_cache").also { it.mkdirs() }
    }

    fun get(referenceId: Long?, page0: Int, ocrLanguage: Languages?): String? {
        val key = key(referenceId, page0, ocrLanguage) ?: return null
        memory.get(key)?.let { return it }
        val file = File(root, fileName(key))
        if (!file.exists()) return null
        return try {
            val text = file.readText()
            if (text.isNotBlank()) {
                memory.put(key, text)
                text
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun put(referenceId: Long?, page0: Int, ocrLanguage: Languages?, text: String) {
        if (text.isBlank()) return
        val key = key(referenceId, page0, ocrLanguage) ?: return
        memory.put(key, text)
        try {
            File(root, fileName(key)).writeText(text)
        } catch (_: Exception) {
        }
    }

    fun clearAll() {
        memory.evictAll()
        try {
            root.deleteRecursively()
            root.mkdirs()
        } catch (_: Exception) {
        }
    }

    private fun key(referenceId: Long?, page0: Int, ocrLanguage: Languages?): String? {
        val id = referenceId ?: return null
        val lang = ocrLanguage?.name ?: "unknown"
        return "${id}_${page0}_$lang"
    }

    private fun fileName(key: String): String =
        key.replace(Regex("[^A-Za-z0-9_\\-]"), "_") + ".txt"

    companion object {
        @Volatile
        private var INSTANCE: OcrPageCache? = null

        fun getInstance(context: Context): OcrPageCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OcrPageCache(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
