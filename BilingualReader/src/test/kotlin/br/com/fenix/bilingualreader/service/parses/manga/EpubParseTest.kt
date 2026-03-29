package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.service.parses.ParserBaseTest
import br.com.fenix.bilingualreader.service.parses.mock.ParseMock
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class EpubParseTest : ParserBaseTest() {

    @Test
    fun testEpubParse() {
        val epubFile = File(testDir, "test_manga.epub")
        val entries = ParseMock.createMockEpubEntries()
        ParseMock.createZip(epubFile, entries)

        val epubParse = EpubParse()
        epubParse.parse(epubFile)

        // numPages should be 2 (page1.jpg, page2.jpg)
        assertEquals(2, epubParse.numPages())

        // isComicInfo should be true
        assertTrue(epubParse.isComicInfo())
        val comicInfo = epubParse.getComicInfo()
        assertNotNull(comicInfo)
        assertEquals("Mock Epub Title", comicInfo?.title)
        assertEquals("Mock Author", comicInfo?.writer)
        assertEquals("Mock Series", comicInfo?.series)

        // getPagePaths should have the chapters from nav.xhtml
        val paths = epubParse.getPagePaths()
        // In createMockEpubEntries, nav.xhtml links page1.jpg as "Chapter 1"
        // EpubParse.parse sets mChapters[item.text()] = pages[href]
        // pages[href] is the index in mPages.
        assertTrue(paths.containsKey("Chapter 1"))
        assertEquals(0, paths["Chapter 1"])

        epubParse.destroy(false)
    }

    @Test(expected = Exception::class)
    fun testInvalidEpub() {
        val epubFile = File(testDir, "invalid.epub")
        val entries = mapOf("mimetype" to "text/plain")
        ParseMock.createZip(epubFile, entries)

        val epubParse = EpubParse()
        epubParse.parse(epubFile)
    }
}
