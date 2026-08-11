package br.com.fenix.bilingualreader.service.llm

import android.content.Context
import android.graphics.BitmapFactory
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.controller.SubTitleController
import br.com.fenix.bilingualreader.service.ocr.OcrFacade
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.translate.MlKitTranslator
import br.com.fenix.bilingualreader.util.helpers.UserLanguageHelper
import br.com.fenix.bilingualreader.util.helpers.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MangaContextProvider(
    private val context: Context,
    private val parse: Parse?,
    private val title: String,
    private val currentPage0Based: Int,
    private val ocrLanguage: Languages? = null,
    private val referenceId: Long? = null
) {
    suspend fun build(radius: Int = 2): ReadingContext {
        val pageCount = parse?.numPages() ?: Int.MAX_VALUE
        val from = (currentPage0Based - radius).coerceAtLeast(0)
        val to = (currentPage0Based + radius).coerceAtMost(pageCount - 1)
        return build((from..to).toList())
    }

    /**
     * Builds context for the given 0-based page indices.
     * Prefer subtitle text per page; fall back to OCR when missing.
     */
    suspend fun build(selectedPages: List<Int>): ReadingContext = withContext(Dispatchers.IO) {
        val userLanguage = UserLanguageHelper.getUserLanguage(context)
        val pages = selectedPages.distinct().sorted().filter { it >= 0 }
        if (pages.isEmpty()) {
            return@withContext ReadingContext(
                type = Type.MANGA,
                title = title,
                sourceLanguage = ocrLanguage,
                userLanguage = userLanguage,
                chunks = emptyList(),
                source = ContextSource.EMPTY
            )
        }

        val subtitleController = SubTitleController.getInstance(context)
        val subtitleByPage = subtitleController.collectSubtitleTextsForPages(pages)
            .associate { pair ->
                val page1 = pair.first.removePrefix("Page ").toIntOrNull()
                val page0 = if (page1 != null) page1 - 1 else -1
                page0 to pair.second
            }
            .filterKeys { it >= 0 }

        val chunks = mutableListOf<ContextChunk>()
        var usedOcr = false
        val translator = MlKitTranslator.getInstance(context)
        val ocr = OcrFacade.getInstance()
        val cache = OcrPageCache.getInstance(context)
        val pageCount = parse?.numPages() ?: 0

        for (page in pages) {
            val subtitle = subtitleByPage[page]
            if (!subtitle.isNullOrBlank()) {
                val imageBase64 = getPageImageBase64(page)
                chunks.add(ContextChunk("Page ${page + 1}", subtitle, page + 1, imageBase64))
                continue
            }

            if (parse == null || page >= pageCount) continue

            val cached = cache.get(referenceId, page, ocrLanguage)
            if (!cached.isNullOrBlank()) {
                val imageBase64 = getPageImageBase64(page)
                chunks.add(ContextChunk("Page ${page + 1}", cached, page + 1, imageBase64))
                usedOcr = true
                continue
            }

            var stream: java.io.InputStream? = null
            try {
                stream = parse.getPage(page)
                val bitmap = BitmapFactory.decodeStream(stream) ?: continue
                val imageBase64 = bitmapToBase64(bitmap)
                val result = ocr.recognize(bitmap, ocrLanguage)
                if (result.fullText.isBlank()) continue
                val (text, _) = translator.translateIfNeeded(result.fullText, ocrLanguage, userLanguage)
                cache.put(referenceId, page, ocrLanguage, text)
                chunks.add(ContextChunk("Page ${page + 1}", text, page + 1, imageBase64))
                usedOcr = true
            } catch (_: Exception) {
            } finally {
                Util.closeInputStream(stream)
            }
        }

        val source = when {
            chunks.isEmpty() -> ContextSource.EMPTY
            usedOcr -> ContextSource.OCR
            else -> ContextSource.SUBTITLES
        }

        ReadingContext(
            type = Type.MANGA,
            title = title,
            sourceLanguage = if (usedOcr) ocrLanguage else subtitleController.getLanguage(),
            userLanguage = userLanguage,
            chunks = chunks,
            source = source
        )
    }

    private fun bitmapToBase64(bitmap: android.graphics.Bitmap): String? {
        return try {
            val maxDim = 800
            val width = bitmap.width
            val height = bitmap.height
            val resized = if (width > maxDim || height > maxDim) {
                val ratio = width.toFloat() / height.toFloat()
                val (newW, newH) = if (ratio > 1f) {
                    maxDim to (maxDim / ratio).toInt()
                } else {
                    (maxDim * ratio).toInt() to maxDim
                }
                android.graphics.Bitmap.createScaledBitmap(bitmap, newW, newH, true)
            } else {
                bitmap
            }
            val out = java.io.ByteArrayOutputStream()
            resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 65, out)
            val bytes = out.toByteArray()
            if (resized != bitmap) resized.recycle()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    private fun getPageImageBase64(page: Int): String? {
        val p = parse ?: return null
        if (page >= p.numPages()) return null
        var stream: java.io.InputStream? = null
        try {
            stream = p.getPage(page)
            val bitmap = BitmapFactory.decodeStream(stream) ?: return null
            val base64 = bitmapToBase64(bitmap)
            bitmap.recycle()
            return base64
        } catch (_: Exception) {
            return null
        } finally {
            Util.closeInputStream(stream)
        }
    }
}
