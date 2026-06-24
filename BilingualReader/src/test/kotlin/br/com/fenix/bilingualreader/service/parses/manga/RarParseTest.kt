package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.service.parses.ParserBaseTest
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.File

class RarParseTest : ParserBaseTest() {

    @Test
    fun testRarParse() {
        val rarFile = File(testDir, "test_manga.rar")
        rarFile.createNewFile()

        val header1 = mockk<FileHeader>()
        every { header1.isDirectory } returns false
        every { header1.fileName } returns "page01.jpg"

        val header2 = mockk<FileHeader>()
        every { header2.isDirectory } returns false
        every { header2.fileName } returns "chapter1/page02.jpg"

        val subHeader = mockk<FileHeader>()
        every { subHeader.isDirectory } returns false
        every { subHeader.fileName } returns "vocabulary.json"

        mockkConstructor(Archive::class)
        every { anyConstructed<Archive>().nextFileHeader() } returnsMany listOf(header1, header2, subHeader, null)
        every { anyConstructed<Archive>().getInputStream(header1) } returns ByteArrayInputStream("image1".toByteArray())
        every { anyConstructed<Archive>().getInputStream(header2) } returns ByteArrayInputStream("image2".toByteArray())
        every { anyConstructed<Archive>().getInputStream(subHeader) } returns ByteArrayInputStream("{\"word\": \"rar\"}".toByteArray())
        
        val rarParse = RarParse()
        rarParse.parse(rarFile)

        // numPages should be 2 (images only)
        assertEquals(2, rarParse.numPages())

        // verify sorting and path logic
        assertEquals("page01.jpg", rarParse.getPagePath(0))
        assertEquals("chapter1/page02.jpg", rarParse.getPagePath(1))

        assertTrue(rarParse.hasSubtitles())
        val subtitles = rarParse.getSubtitles()
        assertEquals(1, subtitles.size)
        assertTrue(subtitles[0].contains("rar"))

        rarParse.destroy(false)
    }

    @Test
    fun testRarWithCache() {
        val rarFile = File(testDir, "test_cache.rar")
        rarFile.createNewFile()
        val cacheDir = File(testDir, "cache") // Use testDir for isolation

        val header = mockk<FileHeader>()
        every { header.isDirectory } returns false
        every { header.fileName } returns "cached_page.jpg"

        mockkConstructor(Archive::class)
        every { anyConstructed<Archive>().nextFileHeader() } returnsMany listOf(header, null)
        every { anyConstructed<Archive>().mainHeader.isSolid } returns false
        
        // Mock extractFile to simulate writing to the cache file
        every { anyConstructed<Archive>().extractFile(header, any()) } answers {
            val os = secondArg<java.io.OutputStream>()
            os.write("cached content".toByteArray())
        }

        val rarParse = RarParse()
        rarParse.setCacheDirectory(cacheDir)
        rarParse.parse(rarFile)

        val stream = rarParse.getPage(0)
        val content = stream.bufferedReader().use { it.readText() }
        assertEquals("cached content", content)

        // Verify file was created in cache
        assertTrue(cacheDir.exists(), "Cache dir should exist")
        assertEquals(1, cacheDir.listFiles()?.size ?: 0)

        rarParse.destroy(true)
        // Verify cache was cleared
        assertFalse(cacheDir.exists(), "Cache directory should have been deleted")
    }
}
