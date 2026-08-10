package br.com.fenix.bilingualreader.service.llm

import android.content.Context
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.util.helpers.UserLanguageHelper

class BookContextProvider(
    private val context: Context,
    private val parse: DocumentParse,
    private val title: String,
    private val currentPage0Based: Int,
    private val sourceLanguage: Languages? = null
) {
    fun build(includeCurrentPage: Boolean = true): ReadingContext {
        val ranges = BookTextExtractor.buildChapterRanges(parse)
        val lastThree = BookTextExtractor.selectLastChapters(ranges, currentPage0Based + 1, 3)
        return build(lastThree, includeCurrentPage)
    }

    fun build(
        selectedRanges: List<BookTextExtractor.ChapterRange>,
        includeCurrentPage: Boolean = true
    ): ReadingContext {
        val userLanguage = UserLanguageHelper.getUserLanguage(context)
        val chunks = mutableListOf<ContextChunk>()
        val currentPage1 = currentPage0Based + 1

        for (chapter in selectedRanges) {
            val text = BookTextExtractor.extractChaptersText(parse, listOf(chapter))
            if (text.isNotBlank()) {
                chunks.add(ContextChunk(chapter.title, text, chapter.startPage))
            }
        }

        if (includeCurrentPage) {
            val pageText = BookTextExtractor.extractPageText(parse, currentPage0Based)
            if (pageText.isNotBlank()) {
                chunks.add(ContextChunk("Current page", pageText, currentPage1))
            }
        }

        return ReadingContext(
            type = Type.BOOK,
            title = title,
            sourceLanguage = sourceLanguage,
            userLanguage = userLanguage,
            chunks = chunks,
            source = if (chunks.isEmpty()) ContextSource.EMPTY else ContextSource.BOOK_TEXT
        )
    }

    fun buildLastThreeChaptersText(): String {
        val ranges = BookTextExtractor.buildChapterRanges(parse)
        val lastThree = BookTextExtractor.selectLastChapters(ranges, currentPage0Based + 1, 3)
        return BookTextExtractor.extractChaptersText(parse, lastThree)
    }

    fun selectedChapterTitles(): List<String> {
        val ranges = BookTextExtractor.buildChapterRanges(parse)
        return BookTextExtractor.selectLastChapters(ranges, currentPage0Based + 1, 3).map { it.title }
    }

    fun allChapterRanges(): List<BookTextExtractor.ChapterRange> =
        BookTextExtractor.buildChapterRanges(parse)
}
