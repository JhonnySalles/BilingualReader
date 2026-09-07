package br.com.fenix.bilingualreader.service.llm

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.ContextChunkFtsEntity
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.RAGContextDatabase
import br.com.fenix.bilingualreader.util.helpers.FtsQuerySanitizer
import br.com.fenix.bilingualreader.util.helpers.TextChunker
import br.com.fenix.bilingualreader.util.helpers.TextPreprocessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SmartContextOrchestrator(
    private val context: Context,
    private val baseReadingContext: ReadingContext
) {
    private val db = RAGContextDatabase.getInstance(context)
    private val dao = db.contextChunkDao()

    companion object {
        private val mutex = Mutex()
        @Volatile
        private var lastIndexedFingerprint: String? = null

        private val REGEX_KEY_TERMS_CLEAN = Regex("[^a-z0-9áàâãéèêíïóôõöúçñ\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF\\u3005-\\u3007\\s]")
        private val REGEX_WHITESPACE = Regex("\\s+")

        fun computeFingerprint(readingContext: ReadingContext): String {
            val chunkCount = readingContext.chunks.size
            val totalLength = readingContext.chunks.sumOf { it.text.length }
            val pageHash = readingContext.chunks.fold(1) { acc, c -> 31 * acc + c.pageOrChapter }
            return "${readingContext.type}_${readingContext.title}_${chunkCount}_${totalLength}_${pageHash}"
        }

        fun invalidateIndex() {
            lastIndexedFingerprint = null
        }
    }

    suspend fun prepareIndex() = withContext(Dispatchers.IO) {
        val currentFingerprint = computeFingerprint(baseReadingContext)
        mutex.withLock {
            if (lastIndexedFingerprint == currentFingerprint) {
                return@withContext
            }

            dao.clearAll()

            val ftsEntities = mutableListOf<ContextChunkFtsEntity>()
            var rowIdCounter = 1
            val isManga = baseReadingContext.type == Type.MANGA

            for (chunk in baseReadingContext.chunks) {
                val preprocessed = TextPreprocessor.prepareForLlm(chunk.text, isManga = isManga)
                val textChunks = TextChunker.chunkText(preprocessed)
                for (textChunk in textChunks) {
                    ftsEntities.add(
                        ContextChunkFtsEntity(
                            rowid = rowIdCounter++,
                            chapterTitle = chunk.label,
                            text = textChunk,
                            pageNumber = chunk.pageOrChapter
                        )
                    )
                }
            }

            if (ftsEntities.isNotEmpty()) {
                dao.insertAll(ftsEntities)
            }
            lastIndexedFingerprint = currentFingerprint
        }
    }

    suspend fun getRelevantContext(
        question: String,
        maxChars: Int
    ): ReadingContext = withContext(Dispatchers.IO) {
        if (baseReadingContext.chunks.isEmpty()) {
            return@withContext baseReadingContext
        }

        val sanitizedQuery = FtsQuerySanitizer.sanitize(question)
        var results = dao.searchChunks(sanitizedQuery, limit = 5)

        if (results.isEmpty()) {
            val individualTerms = extractKeyTerms(question)
            if (individualTerms.isNotBlank()) {
                results = dao.searchChunks(individualTerms, limit = 5)
            }
        }

        if (results.isEmpty()) {
            val maxPage = findCurrentPage()
            val nearbyEntities = dao.getAll(limit = 10)
                .sortedBy { kotlin.math.abs(it.pageNumber - maxPage) }
            results = nearbyEntities.take(5)
        }

        if (results.isEmpty()) {
            results = dao.getAll(limit = 3)
        }

        val filteredChunks = mutableListOf<ContextChunk>()
        var currentLength = 0

        for (item in results) {
            val chunkLabel = item.chapterTitle
            val chunkText = item.text
            val chunkLength = chunkLabel.length + chunkText.length + 5

            if (currentLength + chunkLength > maxChars && filteredChunks.isNotEmpty()) {
                break
            }

            val originalBase64 = baseReadingContext.chunks.firstOrNull { it.pageOrChapter == item.pageNumber }?.imageBase64
            filteredChunks.add(ContextChunk(chunkLabel, chunkText, item.pageNumber, originalBase64))
            currentLength += chunkLength
        }

        ReadingContext(
            type = baseReadingContext.type,
            title = baseReadingContext.title,
            sourceLanguage = baseReadingContext.sourceLanguage,
            userLanguage = baseReadingContext.userLanguage,
            chunks = filteredChunks,
            source = if (filteredChunks.isEmpty()) ContextSource.EMPTY else baseReadingContext.source
        )
    }

    private fun extractKeyTerms(question: String): String {
        val normalized = TextPreprocessor.normalizeJapaneseWidth(question)
        val words = normalized.lowercase()
            .replace(REGEX_KEY_TERMS_CLEAN, " ")
            .split(REGEX_WHITESPACE)
            .filter { it.length > 1 }
            .filter { it !in FtsQuerySanitizer.STOPWORDS_SET }
            .distinct()
            .take(3)

        if (words.isEmpty()) return ""
        return words.joinToString(" OR ") { "$it*" }
    }

    private fun findCurrentPage(): Int {
        return baseReadingContext.chunks
            .map { it.pageOrChapter }
            .maxOrNull() ?: 0
    }
}
