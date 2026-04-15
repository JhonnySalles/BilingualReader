package br.com.fenix.bilingualreader.model.enums

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FileTypeTest {

    @Test
    fun `test file type detection by extension`() {
        assertTrue(FileType.CBZ.`is`("manga.cbz"))
        assertTrue(FileType.PDF.`is`("book.pdf"))
        assertTrue(FileType.EPUB.`is`("test.epub"))
        assertTrue(FileType.`isManga`("archive.cbz"))
        assertTrue(FileType.`isBook`("document.pdf"))
        
        // Test case sensitivity and complex extensions
        assertTrue(FileType.CBZ.`is`("MANGA.CBZ"))
        assertTrue(FileType.FB2.`is`("book.fb2.zip"))
    }

    @Test
    fun `test getType from file or string`() {
        assertEquals(FileType.CBZ, FileType.getType("manga.cbz"))
        assertEquals(FileType.RAR, FileType.getType(File("archive.rar")))
        assertEquals(FileType.UNKNOWN, FileType.getType("unknown.xyz"))
    }

    @Test
    fun `test manga and book grouping`() {
        val mangaTypes = FileType.getManga()
        assertTrue(mangaTypes.contains(FileType.CBZ))
        assertTrue(mangaTypes.contains(FileType.EPUB)) // EPUB is both
        assertFalse(mangaTypes.contains(FileType.PDF))

        val bookTypes = FileType.getBook()
        assertTrue(bookTypes.contains(FileType.PDF))
        assertTrue(bookTypes.contains(FileType.EPUB))
        assertFalse(bookTypes.contains(FileType.CBZ))
    }

    @Test
    fun `test mime type generation`() {
        val cbzMimes = FileType.CBZ.getMime()
        assertTrue(cbzMimes.contains("application/cbz"))
        
        val mangaMimeString = FileType.getMimeTypeManga()
        assertTrue(mangaMimeString.contains("application/cbz"))
        assertTrue(mangaMimeString.contains("application/epub+zip"))
    }
}
