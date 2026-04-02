package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.service.parses.ParserBaseTest
import br.com.fenix.bilingualreader.service.parses.mock.ParseMock
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ZipParseTest : ParserBaseTest() {

    @Test
    fun testZipParse() {
        val zipFile = File(testDir, "test_manga.zip")
        val entries = ParseMock.createMockMangaEntries()
        ParseMock.createZip(zipFile, entries)

        val zipParse = ZipParse()
        zipParse.parse(zipFile)

        // numPages should be 3 (page01.jpg, page02.png, chapter1/page03.jpg)
        assertEquals(3, zipParse.numPages())

        // hasSubtitles should be true (vocabulary.json exists)
        assertTrue(zipParse.hasSubtitles())
        val subtitles = zipParse.getSubtitles()
        assertEquals(1, subtitles.size)
        assertTrue(subtitles[0].contains("\"word\": \"test\""))

        // isComicInfo should be true
        assertTrue(zipParse.isComicInfo())
        val comicInfo = zipParse.getComicInfo()
        assertNotNull(comicInfo)
        assertEquals("Mock Title", comicInfo?.title)

        // getPagePaths should have "chapter1"
        val paths = zipParse.getPagePaths()
        assertTrue(paths.containsKey("chapter1"))
        
        // getChapters should have the index of page03.jpg (which is 2 after sorting)
        // Entries sort: page01.jpg (0), page02.png (1), chapter1/page03.jpg (2)
        // Wait, sort is compareBy { folder }.thenComparing { name }
        // Folder of page01 is "". Folder of chapter1/page03 is "chapter1".
        // So "" comes first.
        val chapters = zipParse.getChapters()
        assertEquals(1, chapters.size)
        assertEquals(2, chapters[0])

        zipParse.destroy(false)
    }

    @Test
    fun testZipWithoutSubtitles() {
        val zipFile = File(testDir, "test_manga_no_sub.zip")
        val entries = mapOf("page01.jpg" to "content")
        ParseMock.createZip(zipFile, entries)

        val zipParse = ZipParse()
        zipParse.parse(zipFile)

        assertFalse(zipParse.hasSubtitles())
        assertEquals(0, zipParse.getSubtitles().size)
        assertFalse(zipParse.isComicInfo())
        assertNull(zipParse.getComicInfo())

        zipParse.destroy(false)
    }
}
