package br.com.fenix.bilingualreader.service.llm

import android.content.Context
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.LlmSettings
import br.com.fenix.bilingualreader.util.helpers.UserLanguageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ChapterSummaryService(private val context: Context) {

    fun cacheKey(referenceId: Long, chapterEnd: Int, selectionKey: String): String {
        val version = GeneralConsts.getSharedPreferences(context)
            .getString(
                GeneralConsts.KEYS.LLM.MODEL_VERSION,
                GeneralConsts.KEYS.LLM.DEFAULT_MODEL_VERSION
            )
        val provider = LlmSettings.effectiveProvider(context).prefValue
        val model = LlmSettings.openRouterModelBookSummary(context)
            .replace(Regex("[^A-Za-z0-9_\\-.]"), "_")
            .take(40)
        val safeSelection = selectionKey.replace(Regex("[^A-Za-z0-9_\\-]"), "_").take(80)
        return "${GeneralConsts.KEYS.LLM.SUMMARY_CACHE_PREFIX}${referenceId}_${chapterEnd}_${safeSelection}_${version}_${provider}_$model"
    }

    fun getCachedSummary(referenceId: Long, chapterEnd: Int, selectionKey: String): String? {
        return GeneralConsts.getSharedPreferences(context)
            .getString(cacheKey(referenceId, chapterEnd, selectionKey), null)
    }

    fun saveCachedSummary(referenceId: Long, chapterEnd: Int, selectionKey: String, summary: String) {
        GeneralConsts.getSharedPreferences(context).edit()
            .putString(cacheKey(referenceId, chapterEnd, selectionKey), summary)
            .apply()
    }

    suspend fun prepareChaptersText(
        parse: DocumentParse,
        currentPage0Based: Int
    ): Pair<String, List<String>> = withContext(Dispatchers.IO) {
        val provider = BookContextProvider(context, parse, "", currentPage0Based)
        provider.buildLastThreeChaptersText() to provider.selectedChapterTitles()
    }

    suspend fun prepareChaptersText(
        parse: DocumentParse,
        ranges: List<BookTextExtractor.ChapterRange>
    ): Pair<String, List<String>> = withContext(Dispatchers.IO) {
        val text = BookTextExtractor.extractChaptersText(parse, ranges)
        text to ranges.map { it.title }
    }

    suspend fun prepareMangaPagesText(
        parse: Parse?,
        title: String,
        currentPage0Based: Int,
        pages: List<Int>,
        ocrLanguage: Languages?,
        referenceId: Long? = null
    ): String = withContext(Dispatchers.IO) {
        MangaContextProvider(context, parse, title, currentPage0Based, ocrLanguage, referenceId)
            .build(pages)
            .joinedText()
    }

    fun summarizeStreaming(
        title: String,
        chaptersText: String,
        userLanguage: Languages,
        referenceId: Long?,
        chapterEnd: Int,
        selectionKey: String
    ): Flow<Pair<String, Boolean>> = flow {
        if (chaptersText.isBlank()) {
            emit("" to true)
            return@flow
        }

        referenceId?.let { id ->
            getCachedSummary(id, chapterEnd, selectionKey)?.let { cached ->
                emit(cached to true)
                return@flow
            }
        }

        val maxChars = UserLanguageHelper.maxContextChars(context)
        val request = LlmPromptBuilder.buildSummaryRequest(title, chaptersText, userLanguage, maxChars)
        val backend = LlmBackendFactory.resolve(context, br.com.fenix.bilingualreader.model.enums.LlmUse.SUMMARY)
        backend.ensureReady()

        var last = ""
        backend.generateStreaming(request).collect { (text, done) ->
            last = text
            emit(text to done)
            if (done && referenceId != null && text.isNotBlank()) {
                saveCachedSummary(referenceId, chapterEnd, selectionKey, text)
            }
        }
        if (last.isEmpty()) emit("" to true)
    }.flowOn(Dispatchers.IO)

    companion object {
        fun selectionKeyFromTitles(titles: List<String>): String =
            titles.joinToString("|").ifBlank { "none" }

        fun selectionKeyFromPages(pages: List<Int>): String {
            val sorted = pages.distinct().sorted()
            if (sorted.isEmpty()) return "none"
            val contiguous = sorted.zipWithNext().all { (a, b) -> b == a + 1 }
            return if (contiguous && sorted.size > 1) {
                "${sorted.first()}-${sorted.last()}"
            } else {
                sorted.joinToString("_")
            }
        }
    }
}
