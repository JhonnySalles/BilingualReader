package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.util.helpers.TextUtil
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class BookSearchTest {

    @Before
    fun setUp() {
        mockkObject(TextUtil)
    }

    @After
    fun tearDown() {
        unmockkObject(TextUtil)
    }

    @Test
    fun `test book search construction with various constructors`() {
        val now = LocalDateTime.now()
        val bs = BookSearch(id = 1L, id_book = 10L, search = "Query", date = now)

        assertEquals(1L, bs.id)
        assertEquals(10L, bs.id_book)
        assertEquals("Query", bs.search)
        assertEquals(now, bs.date)
    }

    @Test
    fun `test content constructor linking to parent`() {
        val parent = BookSearch(1L, "Parent Text", 1.5f)
        val child = BookSearch(1L, "Child Text", 10, parent)

        assertEquals("Parent Text", child.parent?.search)
        assertEquals(1.5f, child.chapter)
        assertEquals(10, child.page)
    }

    @Test
    fun `test toAnnotation`() {
        val parent = BookSearch(1L, "Ch Title", 2.0f)
        val bs = BookSearch(1L, "Found Word", 25, parent)

        every { TextUtil.clearHighlightWordInText("Ch Title") } returns "Ch Title"
        every { TextUtil.clearHighlightWordInText("Found Word") } returns "Found Word"

        val annotation = bs.toAnnotation(pages = 100, fontSize = 14f)

        assertEquals(1L, annotation.id_parent)
        assertEquals(25, annotation.page)
        assertEquals(100, annotation.pages)
        assertEquals(14f, annotation.fontSize)
        assertEquals(MarkType.BookMark, annotation.markType)
        assertEquals(2.0f, annotation.chapterNumber)
        assertEquals("Ch Title", annotation.chapter)
        assertEquals("Found Word", annotation.text)
    }
}
