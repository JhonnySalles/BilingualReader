package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.Languages
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.time.LocalDateTime

class LinkedFileTest {

    @Test
    fun `test linked file construction and ignore constructors`() {
        val now = LocalDateTime.now()
        val lf = LinkedFile(
            id = 1L, idManga = 10L, pages = 50, path = "/path", name = "name", type = "cbz", folder = "/folder",
            language = Languages.JAPANESE, dateCreate = now, lastAccess = now, lastAlteration = now
        )

        assertEquals(1L, lf.id)
        assertEquals(10L, lf.idManga)
        assertEquals(50, lf.pages)
        assertEquals("/path", lf.path)
        assertEquals(Languages.JAPANESE, lf.language)
    }

    @Test
    fun `test addManga updates metadata`() {
        val manga = Manga(1L, 5L, File("/path/manga"))
        val lf = LinkedFile(manga)

        assertEquals(5L, lf.idManga)
        assertEquals(manga, lf.manga)
    }

    @Test
    fun `test clear resets fields`() {
        val lf = LinkedFile(id = 1L, idManga = 10L, pages = 50, path = "P", name = "N", type = "T", folder = "F", language = Languages.ENGLISH, null, null, null)
        
        lf.clear()

        assertNull(lf.id)
        assertEquals(0, lf.pages)
        assertEquals("", lf.path)
        assertEquals(Languages.PORTUGUESE, lf.language)
        assertNull(lf.pagesLink)
    }

    @Test
    fun `test equality uses subset of fields`() {
        val lf1 = LinkedFile(1L, 10L, 5, "P", "N", "T", "F", Languages.ENGLISH, null, null, null)
        val lf2 = LinkedFile(1L, 10L, 5, "P", "N", "T", "F", Languages.PORTUGUESE, null, null, null)
        val lf3 = LinkedFile(2L, 10L, 5, "P", "N", "T", "F", Languages.ENGLISH, null, null, null)

        assertEquals("Different language shouldn't affect equality", lf1, lf2)
        assertNotEquals("Different id or path should affect equality", lf1, lf3)
        assertEquals(lf1.hashCode(), lf2.hashCode())
    }
}
