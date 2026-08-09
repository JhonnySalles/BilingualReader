package br.com.fenix.bilingualreader.service.llm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BookTextExtractorTest {

    @Test
    fun htmlToPlainTextStripsTags() {
        val plain = BookTextExtractor.htmlToPlainText("<p>Hello <b>world</b></p>")
        assertEquals("Hello world", plain)
    }

    @Test
    fun selectLastChaptersTakesPreviousThree() {
        val ranges = listOf(
            BookTextExtractor.ChapterRange("C1", 1, 10),
            BookTextExtractor.ChapterRange("C2", 10, 20),
            BookTextExtractor.ChapterRange("C3", 20, 30),
            BookTextExtractor.ChapterRange("C4", 30, 40),
            BookTextExtractor.ChapterRange("C5", 40, 50)
        )
        val selected = BookTextExtractor.selectLastChapters(ranges, currentPage1Based = 45, count = 3)
        assertEquals(listOf("C2", "C3", "C4"), selected.map { it.title })
    }

    @Test
    fun selectLastChaptersAtStartUsesAvailable() {
        val ranges = listOf(
            BookTextExtractor.ChapterRange("C1", 1, 10),
            BookTextExtractor.ChapterRange("C2", 10, 20)
        )
        val selected = BookTextExtractor.selectLastChapters(ranges, currentPage1Based = 5, count = 3)
        assertEquals(listOf("C1"), selected.map { it.title })
    }

    @Test
    fun findCurrentChapterIndex() {
        val ranges = listOf(
            BookTextExtractor.ChapterRange("C1", 1, 10),
            BookTextExtractor.ChapterRange("C2", 10, 20)
        )
        assertEquals(0, BookTextExtractor.findCurrentChapterIndex(ranges, 5))
        assertEquals(1, BookTextExtractor.findCurrentChapterIndex(ranges, 15))
        assertTrue(BookTextExtractor.findCurrentChapterIndex(emptyList(), 1) < 0)
    }
}
