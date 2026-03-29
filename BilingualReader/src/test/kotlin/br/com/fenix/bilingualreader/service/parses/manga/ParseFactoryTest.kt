package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.service.parses.ParserBaseTest
import br.com.fenix.bilingualreader.service.parses.mock.ParseMock
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ParseFactoryTest : ParserBaseTest() {

    @Test
    fun testCreateZip() {
        val zipFile = File(testDir, "test.zip")
        ParseMock.createZip(zipFile, mapOf("1.jpg" to "", "2.jpg" to "", "3.jpg" to "", "4.jpg" to ""))
        val parser = ParseFactory.create(zipFile)
        assertTrue(parser is ZipParse)
    }

    @Test
    fun testCreateCbz() {
        val cbzFile = File(testDir, "test.cbz")
        ParseMock.createZip(cbzFile, mapOf("1.jpg" to "", "2.jpg" to "", "3.jpg" to "", "4.jpg" to ""))
        val parser = ParseFactory.create(cbzFile)
        assertTrue(parser is ZipParse)
    }

    @Test
    fun testCreateTar() {
        val tarFile = File(testDir, "test.tar")
        ParseMock.createTar(tarFile, mapOf("1.jpg" to "", "2.jpg" to "", "3.jpg" to "", "4.jpg" to ""))
        val parser = ParseFactory.create(tarFile)
        assertTrue(parser is TarParse)
    }

    @Test
    fun testCreate7z() {
        val sevenZFile = File(testDir, "test.7z")
        ParseMock.create7z(sevenZFile, mapOf("1.jpg" to "", "2.jpg" to "", "3.jpg" to "", "4.jpg" to ""))
        val parser = ParseFactory.create(sevenZFile)
        assertTrue(parser is SevenZipParse)
    }

    @Test
    fun testCreateEpub() {
        val epubFile = File(testDir, "test.epub")
        ParseMock.createZip(epubFile, ParseMock.createMockEpubEntries())
        val parser = ParseFactory.create(epubFile)
        assertTrue(parser is EpubParse)
    }

    @Test
    fun testCreateDirectory() {
        val mangaDir = tempFolder.newFolder("manga_factory")
        for (i in 1..5) File(mangaDir, "page$i.jpg").writeText("")
        
        val parser = ParseFactory.create(mangaDir)
        assertTrue(parser is DirectoryParse)
    }

    @Test
    fun testCreateDirectorySmall() {
        val mangaDir = tempFolder.newFolder("manga_small")
        for (i in 1..2) File(mangaDir, "page$i.jpg").writeText("")
        
        // factory returns null if DirectoryParse has < 4 pages
        val parser = ParseFactory.create(mangaDir)
        assertNull(parser)
    }

    @Test
    fun testCreateUnsupported() {
        val unkFile = File(testDir, "test.unknown")
        unkFile.writeText("content")
        val parser = ParseFactory.create(unkFile)
        assertNull(parser)
    }
}
