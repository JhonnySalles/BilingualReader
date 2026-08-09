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
    private val ocrLanguage: Languages? = null
) {
    suspend fun build(radius: Int = 2): ReadingContext = withContext(Dispatchers.IO) {
        val userLanguage = UserLanguageHelper.getUserLanguage(context)
        val subtitleController = SubTitleController.getInstance(context)
        val subtitlePairs = subtitleController.collectNearbySubtitleTexts(radius)

        if (subtitlePairs.isNotEmpty()) {
            val chunks = subtitlePairs.map { (label, text) ->
                ContextChunk(label, text)
            }
            return@withContext ReadingContext(
                type = Type.MANGA,
                title = title,
                sourceLanguage = subtitleController.getLanguage(),
                userLanguage = userLanguage,
                chunks = chunks,
                source = ContextSource.SUBTITLES
            )
        }

        if (parse == null) {
            return@withContext ReadingContext(
                type = Type.MANGA,
                title = title,
                sourceLanguage = ocrLanguage,
                userLanguage = userLanguage,
                chunks = emptyList(),
                source = ContextSource.EMPTY
            )
        }

        val translator = MlKitTranslator.getInstance(context)
        val ocr = OcrFacade.getInstance()
        val chunks = mutableListOf<ContextChunk>()
        val from = (currentPage0Based - radius).coerceAtLeast(0)
        val to = (currentPage0Based + radius).coerceAtMost(parse.numPages() - 1)

        for (page in from..to) {
            var stream: java.io.InputStream? = null
            try {
                stream = parse.getPage(page)
                val bitmap = BitmapFactory.decodeStream(stream) ?: continue
                val result = ocr.recognize(bitmap, ocrLanguage)
                if (result.fullText.isBlank()) continue
                val (text, _) = translator.translateIfNeeded(result.fullText, ocrLanguage, userLanguage)
                chunks.add(ContextChunk("Page ${page + 1}", text, page + 1))
            } catch (_: Exception) {
            } finally {
                Util.closeInputStream(stream)
            }
        }

        ReadingContext(
            type = Type.MANGA,
            title = title,
            sourceLanguage = ocrLanguage,
            userLanguage = userLanguage,
            chunks = chunks,
            source = if (chunks.isEmpty()) ContextSource.EMPTY else ContextSource.OCR
        )
    }
}
