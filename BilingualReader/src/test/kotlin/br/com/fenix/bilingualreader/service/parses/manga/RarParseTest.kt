package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.service.parses.ParserBaseTest
import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

class RarParseTest : ParserBaseTest() {

    @Before
    override fun setUp() {
        super.setUp()
    }

    @After
    override fun tearDown() {
        super.tearDown()
        unmockkConstructor(Archive::class)
    }

    @Test
    fun testRarParse() {
        val rarFile = File(testDir, "test_manga.rar")
        // Use a valid RAR signature to avoid constructor crash in some versions
        rarFile.writeBytes(byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00))

        val header1 = mockk<FileHeader>(relaxed = true)
        val header2 = mockk<FileHeader>(relaxed = true)
        val headerSub = mockk<FileHeader>(relaxed = true)
        val headerInfo = mockk<FileHeader>(relaxed = true)

        every { header1.isDirectory } returns false
        every { header1.fileName } returns "page01.jpg"
        every { header2.isDirectory } returns false
        every { header2.fileName } returns "page02.png"
        every { headerSub.isDirectory } returns false
        every { headerSub.fileName } returns "vocabulary.json"
        every { headerInfo.isDirectory } returns false
        every { headerInfo.fileName } returns "ComicInfo.xml"

        val headers = mutableListOf(header1, header2, headerSub, headerInfo)
        var index = 0
        
        val subContent = "{\"word\": \"test\"}"
        val infoContent = """
            <?xml version="1.0"?>
            <ComicInfo xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
              <Title>Mock Title</Title>
            </ComicInfo>
        """.trimIndent()

        mockkConstructor(Archive::class)
        every { anyConstructed<Archive>().nextFileHeader() } answers {
            if (index < headers.size) headers[index++] else null
        }
        every { anyConstructed<Archive>().getInputStream(headerSub) } returns ByteArrayInputStream(subContent.toByteArray())
        every { anyConstructed<Archive>().getInputStream(headerInfo) } returns ByteArrayInputStream(infoContent.toByteArray())
        every { anyConstructed<Archive>().mainHeader } returns mockk(relaxed = true)
        every { anyConstructed<Archive>().close() } returns Unit

        val rarParse = RarParse()
        rarParse.parse(rarFile)

        assertEquals(2, rarParse.numPages())
        assertTrue(rarParse.hasSubtitles())
        assertEquals(1, rarParse.getSubtitles().size)
        assertTrue(rarParse.isComicInfo())
        assertNotNull(rarParse.getComicInfo())
        assertEquals("Mock Title", rarParse.getComicInfo()?.title)

        rarParse.destroy(false)
    }
}
