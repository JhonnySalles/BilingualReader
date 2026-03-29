package br.com.fenix.bilingualreader.service.parses.manga

import br.com.fenix.bilingualreader.service.parses.ParserBaseTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException

class DirectoryParseTest : ParserBaseTest() {

    @Test
    fun testDirectoryParse() {
        val mangaDir = tempFolder.newFolder("manga_dir")
        File(mangaDir, "page01.jpg").writeText("image 1")
        File(mangaDir, "page02.png").writeText("image 2")
        File(mangaDir, "vocabulary.json").writeText("{\"word\": \"test\"}")
        File(mangaDir, "ComicInfo.xml").writeText("""
            <?xml version="1.0"?>
            <ComicInfo>
              <Title>Mock Directory Title</Title>
            </ComicInfo>
        """.trimIndent())

        val directoryParse = DirectoryParse()
        directoryParse.parse(mangaDir)

        assertEquals(2, directoryParse.numPages())
        assertTrue(directoryParse.hasSubtitles())
        assertEquals(1, directoryParse.getSubtitles().size)
        assertTrue(directoryParse.isComicInfo())
        assertEquals("Mock Directory Title", directoryParse.getComicInfo()?.title)

        directoryParse.destroy(false)
    }

    @Test(expected = IOException::class)
    fun testParseNotDirectory() {
        val file = createSampleFile("not_a_dir.txt")
        val directoryParse = DirectoryParse()
        directoryParse.parse(file)
    }

    @Test(expected = IOException::class)
    fun testParseWithSubDirectory() {
        val mangaDir = tempFolder.newFolder("manga_with_sub")
        tempFolder.newFolder("manga_with_sub", "sub_dir")
        
        val directoryParse = DirectoryParse()
        directoryParse.parse(mangaDir)
    }
}
