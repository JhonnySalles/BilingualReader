package br.com.fenix.bilingualreader.service.llm

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.controller.SubTitleController
import br.com.fenix.bilingualreader.service.ocr.OcrFacade
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.translate.MlKitTranslator
import br.com.fenix.bilingualreader.util.helpers.TextPreprocessor
import br.com.fenix.bilingualreader.util.helpers.TextQualityValidator
import br.com.fenix.bilingualreader.util.helpers.UserLanguageHelper
import br.com.fenix.bilingualreader.util.helpers.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
     * Prefers subtitle text per page; falls back to cached/parallel OCR when missing.
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

        val translator = MlKitTranslator.getInstance(context)
        val ocr = OcrFacade.getInstance()
        val cache = OcrPageCache.getInstance(context)
        val pageCount = parse?.numPages() ?: 0

        // Limit concurrent OCR tasks to max 3 to prevent CPU/memory saturation
        val ocrSemaphore = Semaphore(3)
        var usedOcr = false

        val deferredChunks = pages.map { page ->
            async {
                val subtitle = subtitleByPage[page]
                if (!subtitle.isNullOrBlank()) {
                    val cleanSub = TextPreprocessor.prepareForLlm(subtitle, isManga = true)
                    val imageBase64 = getPageImageBase64(page)
                    return@async ContextChunk("Page ${page + 1}", cleanSub, page + 1, imageBase64)
                }

                if (parse == null || page >= pageCount) return@async null

                val cached = cache.get(referenceId, page, ocrLanguage)
                if (!cached.isNullOrBlank() && TextQualityValidator.isReadable(cached)) {
                    val cleanCached = TextPreprocessor.prepareForLlm(cached, isManga = true)
                    val imageBase64 = getPageImageBase64(page)
                    usedOcr = true
                    return@async ContextChunk("Page ${page + 1}", cleanCached, page + 1, imageBase64)
                }

                ocrSemaphore.withPermit {
                    var stream: java.io.InputStream? = null
                    var bitmap: Bitmap? = null
                    try {
                        stream = parse.getPage(page) ?: return@withPermit null
                        bitmap = decodeSampledBitmap(stream, 1600, 1600) ?: return@withPermit null
                        val imageBase64 = bitmapToBase64(bitmap)
                        val result = ocr.recognize(bitmap, ocrLanguage)
                        val rawOcrText = TextPreprocessor.cleanOcrArtifacts(result.fullText)

                        if (rawOcrText.isBlank()) {
                            return@withPermit ContextChunk(
                                "Page ${page + 1}",
                                context.getString(br.com.fenix.bilingualreader.R.string.llm_assistant_no_readable_text),
                                page + 1,
                                imageBase64
                            )
                        }

                        val confidence = TextQualityValidator.confidenceScore(rawOcrText)
                        if (confidence < 0.25f) {
                            return@withPermit ContextChunk(
                                "Page ${page + 1}",
                                context.getString(br.com.fenix.bilingualreader.R.string.llm_assistant_low_confidence_text),
                                page + 1,
                                imageBase64
                            )
                        }

                        val (translated, _) = translator.translateIfNeeded(rawOcrText, ocrLanguage, userLanguage)
                        val cleanText = TextPreprocessor.prepareForLlm(translated, isManga = true)
                        if (!TextQualityValidator.isReadable(cleanText)) {
                            return@withPermit ContextChunk(
                                "Page ${page + 1}",
                                context.getString(br.com.fenix.bilingualreader.R.string.llm_assistant_incoherent_text),
                                page + 1,
                                imageBase64
                            )
                        }

                        cache.put(referenceId, page, ocrLanguage, cleanText)
                        usedOcr = true
                        ContextChunk("Page ${page + 1}", cleanText, page + 1, imageBase64)
                    } catch (_: Exception) {
                        null
                    } finally {
                        bitmap?.recycle()
                        Util.closeInputStream(stream)
                    }
                }
            }
        }

        val chunks = deferredChunks.awaitAll().filterNotNull()

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

    private fun decodeSampledBitmap(stream: java.io.InputStream, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val bytes = stream.readBytes()
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
        } catch (_: Exception) {
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String? {
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
                Bitmap.createScaledBitmap(bitmap, newW, newH, true)
            } else {
                bitmap
            }
            val out = java.io.ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 65, out)
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
        var bitmap: Bitmap? = null
        try {
            stream = p.getPage(page) ?: return null
            bitmap = decodeSampledBitmap(stream, 800, 800) ?: return null
            return bitmapToBase64(bitmap)
        } catch (_: Exception) {
            return null
        } finally {
            bitmap?.recycle()
            Util.closeInputStream(stream)
        }
    }
}
