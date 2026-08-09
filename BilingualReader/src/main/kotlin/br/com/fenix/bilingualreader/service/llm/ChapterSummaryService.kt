package br.com.fenix.bilingualreader.service.llm

import android.content.Context
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.UserLanguageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ChapterSummaryService(private val context: Context) {

    fun cacheKey(bookId: Long, chapterEnd: Int): String {
        val version = GeneralConsts.getSharedPreferences(context)
            .getString(
                GeneralConsts.KEYS.LLM.MODEL_VERSION,
                GeneralConsts.KEYS.LLM.DEFAULT_MODEL_VERSION
            )
        return "${GeneralConsts.KEYS.LLM.SUMMARY_CACHE_PREFIX}${bookId}_${chapterEnd}_$version"
    }

    fun getCachedSummary(bookId: Long, chapterEnd: Int): String? {
        return GeneralConsts.getSharedPreferences(context)
            .getString(cacheKey(bookId, chapterEnd), null)
    }

    fun saveCachedSummary(bookId: Long, chapterEnd: Int, summary: String) {
        GeneralConsts.getSharedPreferences(context).edit()
            .putString(cacheKey(bookId, chapterEnd), summary)
            .apply()
    }

    suspend fun prepareChaptersText(
        parse: DocumentParse,
        currentPage0Based: Int
    ): Pair<String, List<String>> = withContext(Dispatchers.IO) {
        val provider = BookContextProvider(context, parse, "", currentPage0Based)
        provider.buildLastThreeChaptersText() to provider.selectedChapterTitles()
    }

    fun summarizeStreaming(
        title: String,
        chaptersText: String,
        userLanguage: Languages,
        bookId: Long?,
        chapterEnd: Int
    ): Flow<Pair<String, Boolean>> = flow {
        if (chaptersText.isBlank()) {
            emit("" to true)
            return@flow
        }

        bookId?.let { id ->
            getCachedSummary(id, chapterEnd)?.let { cached ->
                emit(cached to true)
                return@flow
            }
        }

        val maxChars = UserLanguageHelper.maxContextChars(context)
        val prompt = LlmPromptBuilder.buildSummaryPrompt(title, chaptersText, userLanguage, maxChars)
        val manager = LlmModelManager.getInstance(context)
        val engine = LlmInferenceEngine.getInstance(context)
        if (!manager.isModelReady()) {
            throw IllegalStateException("Model missing")
        }
        engine.ensureLoaded(manager.getModelFile().absolutePath)

        var last = ""
        engine.generateStreamingTokens(prompt).collect { (text, done) ->
            last = text
            emit(text to done)
            if (done && bookId != null && text.isNotBlank()) {
                saveCachedSummary(bookId, chapterEnd, text)
            }
        }
        if (last.isEmpty()) emit("" to true)
    }.flowOn(Dispatchers.IO)
}
