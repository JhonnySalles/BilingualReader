package br.com.fenix.bilingualreader.service.llm

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.ContextChunkFtsEntity
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.RAGContextDatabase
import br.com.fenix.bilingualreader.util.helpers.FtsQuerySanitizer
import br.com.fenix.bilingualreader.util.helpers.TextChunker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmartContextOrchestrator(
    private val context: Context,
    private val baseReadingContext: ReadingContext
) {
    private val db = RAGContextDatabase.getInstance(context)
    private val dao = db.contextChunkDao()

    suspend fun prepareIndex() = withContext(Dispatchers.IO) {
        dao.clearAll()

        val ftsEntities = mutableListOf<ContextChunkFtsEntity>()
        var rowIdCounter = 1

        for (chunk in baseReadingContext.chunks) {
            val textChunks = TextChunker.chunkText(chunk.text)
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
            // Fallback: take top 3 chunks (e.g. current page / recent chunks)
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
}
