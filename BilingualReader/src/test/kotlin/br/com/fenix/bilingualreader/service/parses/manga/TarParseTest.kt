package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.service.parses.ParserBaseTest
import br.com.fenix.bilingualreader.service.parses.mock.ParseMock
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class TarParseTest : ParserBaseTest() {

    @Test
    fun testTarParse() {
        val tarFile = File(testDir, "test_manga.tar")
        val entries = ParseMock.createMockMangaEntries()
        ParseMock.createTar(tarFile, entries)

        val tarParse = TarParse()
        tarParse.parse(tarFile)

        // numPages should be 3 (page01.jpg, page02.png, chapter1/page03.jpg)
        assertEquals(3, tarParse.numPages())

        // hasSubtitles should be true (vocabulary.json exists)
        assertTrue(tarParse.hasSubtitles())
        val subtitles = tarParse.getSubtitles()
        assertEquals(1, subtitles.size)
        assertTrue(subtitles[0].contains("\"word\": \"test\""))

        // isComicInfo should be true
        assertTrue(tarParse.isComicInfo())
        val comicInfo = tarParse.getComicInfo()
        assertNotNull(comicInfo)
        assertEquals("Mock Title", comicInfo?.title)

        // getPagePaths should have "" and "chapter1"
        val paths = tarParse.getPagePaths()
        assertTrue(paths.containsKey(""))
        assertTrue(paths.containsKey("chapter1"))
        
        // getChapters should have the index of page03.jpg
        val chapters = tarParse.getChapters()
        assertEquals(1, chapters.size)
        assertEquals(2, chapters[0])

        tarParse.destroy(false)
    }

    @Test
    fun testTarWithoutSubtitles() {
        val tarFile = File(testDir, "test_manga_no_sub.tar")
        val entries = mapOf("page01.jpg" to "content")
        ParseMock.createTar(tarFile, entries)

        val tarParse = TarParse()
        tarParse.parse(tarFile)

        assertFalse(tarParse.hasSubtitles())
        assertEquals(0, tarParse.getSubtitles().size)
        assertFalse(tarParse.isComicInfo())
        assertNull(tarParse.getComicInfo())

        tarParse.destroy(false)
    }
}
