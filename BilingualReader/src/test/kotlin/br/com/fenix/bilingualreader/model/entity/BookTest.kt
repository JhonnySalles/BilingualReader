package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Languages
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BookTest {

    private fun createDefaultBook(id: Long? = 1L): Book {
        return Book(
            id = id,
            title = "Original Title",
            author = "Original Author",
            password = "",
            annotation = "Original Annotation",
            release = LocalDate.of(2020, 1, 1),
            genre = "Genre",
            publisher = "Publisher",
            series = "Series",
            isbn = "123456",
            pages = 100,
            volume = "1",
            chapter = 0,
            chapterDescription = "",
            bookMark = 10,
            completed = false,
            language = Languages.ENGLISH,
            path = "/path/to/book.epub",
            folder = "/path/to",
            name = "book.epub",
            fileType = FileType.EPUB,
            fileSize = 1024L,
            favorite = false,
            fkLibrary = 1L,
            tags = mutableListOf(1L, 2L),
            excluded = false,
            dateCreate = LocalDateTime.now(),
            lastAccess = null,
            lastAlteration = null,
            fileAlteration = Date(),
            lastVocabImport = null,
            lastVerify = null
        )
    }

    @Test
    fun `test update basic fields should return true when changed`() {
        val original = createDefaultBook()
        val updateData = createDefaultBook().apply {
            bookMark = 20
            favorite = true
            language = Languages.JAPANESE
        }

        val result = original.update(updateData, isFull = false)

        assertTrue("Update should return true when fields changed", result)
        assertEquals(20, original.bookMark)
        assertTrue(original.favorite)
        assertEquals(Languages.JAPANESE, original.language)
        assertFalse("Completed should be false if bookmark < pages", original.completed)
    }

    @Test
    fun `test update basic fields should return false when no changes`() {
        val original = createDefaultBook()
        val updateData = createDefaultBook()

        val result = original.update(updateData, isFull = false)

        assertFalse("Update should return false when no fields changed", result)
    }

    @Test
    fun `test bookMark logic updates completed status`() {
        val book = createDefaultBook()
        book.pages = 100

        book.bookMark = 50
        assertFalse(book.completed)

        book.bookMark = 100
        assertTrue("Book should be completed when bookmark matches pages", book.completed)

        book.bookMark = 120
        assertTrue("Book should be completed when bookmark exceeds pages", book.completed)
    }

    @Test
    fun `test full update should update all metadata`() {
        val original = createDefaultBook()
        val nextMonth = LocalDate.of(2020, 2, 1)
        val updateData = createDefaultBook().apply {
            title = "New Title"
            author = "New Author"
            annotation = "New Annotation"
            release = nextMonth
            volume = "2"
        }

        val result = original.update(updateData, isFull = true)

        assertTrue(result)
        assertEquals("New Title", original.title)
        assertEquals("New Author", original.author)
        assertEquals("New Annotation", original.annotation)
        assertEquals(nextMonth, original.release)
        assertEquals("2", original.volume)
    }

    @Test
    fun `test update partial should NOT update title or author`() {
        val original = createDefaultBook()
        val updateData = createDefaultBook().apply {
            title = "New Title"
            author = "New Author"
        }

        val result = original.update(updateData, isFull = false)

        assertFalse("Update should return false if only non-tracked fields in partial update changed", result)
        assertNotEquals("New Title", original.title)
        assertNotEquals("New Author", original.author)
    }
}
