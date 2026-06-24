package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.service.parses.ParserBaseTest
import br.com.fenix.bilingualreader.service.parses.mock.ParseMock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class SevenZipParseTest : ParserBaseTest() {

    @Test
    fun testSevenZipParse() {
        val sevenZFile = File(testDir, "test_manga.7z")
        val entries = ParseMock.createMockMangaEntries()
        ParseMock.create7z(sevenZFile, entries)

        val sevenZipParse = SevenZipParse()
        sevenZipParse.parse(sevenZFile)

        // numPages should be 3 (page01.jpg, page02.png, chapter1/page03.jpg)
        assertEquals(3, sevenZipParse.numPages())

        // hasSubtitles should be true (vocabulary.json exists)
        assertTrue(sevenZipParse.hasSubtitles())
        val subtitles = sevenZipParse.getSubtitles()
        assertEquals(1, subtitles.size)
        assertTrue(subtitles[0].contains("\"word\": \"test\""))

        // isComicInfo should be true
        assertTrue(sevenZipParse.isComicInfo())
        val comicInfo = sevenZipParse.getComicInfo()
        assertNotNull(comicInfo)
        assertEquals("Mock Title", comicInfo?.title)

        // getPagePaths should have "" and "chapter1"
        val paths = sevenZipParse.getPagePaths()
        assertTrue(paths.containsKey(""))
        assertTrue(paths.containsKey("chapter1"))
        
        // getChapters should have the index of page03.jpg
        val chapters = sevenZipParse.getChapters()
        assertEquals(1, chapters.size)
        assertEquals(2, chapters[0])

        sevenZipParse.destroy(false)
    }

    @Test
    fun testSevenZipWithoutSubtitles() {
        val sevenZFile = File(testDir, "test_manga_no_sub.7z")
        val entries = mapOf("page01.jpg" to "content")
        ParseMock.create7z(sevenZFile, entries)

        val sevenZipParse = SevenZipParse()
        sevenZipParse.parse(sevenZFile)

        assertFalse(sevenZipParse.hasSubtitles())
        assertEquals(0, sevenZipParse.getSubtitles().size)
        assertFalse(sevenZipParse.isComicInfo())
        assertNull(sevenZipParse.getComicInfo())

        sevenZipParse.destroy(false)
    }
}
