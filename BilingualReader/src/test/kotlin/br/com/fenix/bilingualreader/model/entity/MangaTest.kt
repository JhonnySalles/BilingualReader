package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.FileType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MangaTest {

    private fun createDefaultManga(id: Long? = 1L): Manga {
        return Manga(
            id = id,
            title = "Original Title",
            path = "/path/to/manga.zip",
            folder = "/path/to",
            name = "manga.zip",
            fileSize = 1024L,
            fileType = FileType.ZIP,
            pages = 50,
            chapters = intArrayOf(1, 2),
            chaptersPages = mapOf(1 to "page 1"),
            bookMark = 5,
            completed = false,
            favorite = false,
            hasSubtitle = false,
            author = "Original Author",
            series = "Series",
            genre = "Genre",
            publisher = "Publisher",
            volume = "1",
            release = LocalDate.of(2020, 1, 1),
            fkLibrary = 1L,
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
        val original = createDefaultManga()
        val updateData = createDefaultManga().apply {
            bookMark = 20
            favorite = true
            hasSubtitle = true
        }

        val result = original.update(updateData, isFull = false)

        assertTrue("Update should return true when fields changed", result)
        assertEquals(20, original.bookMark)
        assertTrue(original.favorite)
        assertTrue(original.hasSubtitle)
        assertFalse("Completed should be false if bookmark < pages", original.completed)
    }

    @Test
    fun `test update basic fields should return false when no changes`() {
        val original = createDefaultManga()
        val updateData = createDefaultManga()

        val result = original.update(updateData, isFull = false)

        assertFalse("Update should return false when no fields changed", result)
    }

    @Test
    fun `test bookMark logic updates completed status`() {
        val manga = createDefaultManga()
        manga.pages = 50

        manga.bookMark = 25
        assertFalse(manga.completed)

        manga.bookMark = 50
        assertTrue("Manga should be completed when bookmark matches pages", manga.completed)

        manga.bookMark = 60
        assertTrue("Manga should be completed when bookmark exceeds pages", manga.completed)
    }

    @Test
    fun `test full update should update all metadata`() {
        val original = createDefaultManga()
        val nextMonth = LocalDate.of(2020, 2, 1)
        val updateData = createDefaultManga().apply {
            title = "New Title"
            author = "New Author"
            publisher = "New Publisher"
            release = nextMonth
            volume = "2"
            pages = 60
            chapters = intArrayOf(1, 2, 3)
            chaptersPages = mapOf(1 to "p1", 2 to "p2")
        }

        val result = original.update(updateData, isFull = true)

        assertTrue(result)
        assertEquals("New Title", original.title)
        assertEquals("New Author", original.author)
        assertEquals("New Publisher", original.publisher)
        assertEquals(nextMonth, original.release)
        assertEquals("2", original.volume)
        assertEquals(60, original.pages)
        assertArrayEquals(intArrayOf(1, 2, 3), original.chapters)
        assertEquals(2, original.chaptersPages.size)
    }

    @Test
    fun `test update partial should NOT update title or author or pages`() {
        val original = createDefaultManga()
        val updateData = createDefaultManga().apply {
            title = "New Title"
            author = "New Author"
            pages = 100
        }

        val result = original.update(updateData, isFull = false)

        assertFalse("Update should return false if only non-tracked fields in partial update changed", result)
        assertNotEquals("New Title", original.title)
        assertNotEquals("New Author", original.author)
        assertNotEquals(100, original.pages)
    }
}
