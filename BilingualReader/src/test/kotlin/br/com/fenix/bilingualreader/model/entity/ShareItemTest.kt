package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import com.google.firebase.Timestamp
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.util.Date

class ShareItemTest {

    @Before
    fun setUp() {
        mockkObject(GeneralConsts)
        every { GeneralConsts.dateTimeToDate(any()) } returns Date()
    }

    @After
    fun tearDown() {
        unmockkObject(GeneralConsts)
    }

    private fun createDefaultShareItem(): ShareItem {
        return ShareItem(
            file = "TestFile",
            bookMark = 10,
            pages = 100,
            completed = false,
            favorite = true,
            lastAccess = Date(),
            sync = Date()
        )
    }

    @Test
    fun `test share item from firebase map`() {
        val now = Timestamp.now()
        val firebase = mapOf(
            ShareItem.FIELD_FILE to "FireFile",
            ShareItem.FIELD_BOOKMARK to 20L,
            ShareItem.FIELD_PAGES to 50L,
            ShareItem.FIELD_COMPLETED to true,
            ShareItem.FIELD_FAVORITE to false,
            ShareItem.FIELD_LASTACCESS to now,
            ShareItem.FIELD_SYNC to now
        )

        val item = ShareItem(firebase)

        assertEquals("FireFile", item.file)
        assertEquals(20, item.bookMark)
        assertEquals(50, item.pages)
        assertTrue(item.completed)
        assertFalse(item.favorite)
    }

    @Test
    fun `test share item from manga constructor`() {
        val manga = Manga(1L, 1L, File("Manga")).apply {
            title = "Manga"
            bookMark = 5
            pages = 10
            completed = false
            favorite = true
            fkLibrary = 10L
            lastAccess = LocalDateTime.now()
        }
        val histories = listOf<History>()
        val annotations = listOf<MangaAnnotation>()

        val item = ShareItem(manga, histories, annotations)

        assertEquals("Manga", item.file)
        assertEquals(5, item.bookMark)
        assertTrue(item.alter)
        assertTrue(item.processed)
        assertEquals(1L, item.id)
        assertEquals(10L, item.idLibrary)
    }

    @Test
    fun `test merge updates fields`() {
        val item = createDefaultShareItem()
        val manga = Manga(1L, 1L, File("Manga")).apply {
            title = "Manga"
            bookMark = 80
            pages = 100
            completed = true
            favorite = false
            lastAccess = LocalDateTime.now()
        }

        item.merge(manga)

        assertEquals(80, item.bookMark)
        assertTrue(item.completed)
        assertFalse(item.favorite)
        assertTrue(item.alter)
    }

    @Test
    fun `test refreshHistory`() {
        val item = createDefaultShareItem()
        val now = LocalDateTime.now()
        val histories = listOf(
            History(fkLibrary = 1L, fkReference = 2L, type = Type.MANGA, pageStart = 0, pages = 10, volume = "1").apply {
                // start is set by default
            }
        )

        item.refreshHistory(histories)

        assertEquals(1, item.history?.size)
    }
}
