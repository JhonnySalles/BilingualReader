package br.com.ebook.extractor

import br.com.ebook.core.BookExtractorFactory
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BookExtractorFactoryTest {

    @Test
    fun testGetExtractorForSupportedFormats() {
        val epubExtractor = BookExtractorFactory.getExtractor("test.epub")
        assertNotNull(epubExtractor)
        assertTrue(epubExtractor is EpubBookExtractor)

        val fb2Extractor = BookExtractorFactory.getExtractor("test.fb2")
        assertNotNull(fb2Extractor)
        assertTrue(fb2Extractor is Fb2BookExtractor)

        val mobiExtractor = BookExtractorFactory.getExtractor("test.mobi")
        assertNotNull(mobiExtractor)
        assertTrue(mobiExtractor is MobiBookExtractor)

        val txtExtractor = BookExtractorFactory.getExtractor("test.txt")
        assertNotNull(txtExtractor)
        assertTrue(txtExtractor is TxtBookExtractor)

        val htmlExtractor = BookExtractorFactory.getExtractor("test.html")
        assertNotNull(htmlExtractor)
        assertTrue(htmlExtractor is HtmlBookExtractor)

        val rtfExtractor = BookExtractorFactory.getExtractor("test.rtf")
        assertNotNull(rtfExtractor)
        assertTrue(rtfExtractor is RtfBookExtractor)

        val cbzExtractor = BookExtractorFactory.getExtractor("test.cbz")
        assertNotNull(cbzExtractor)
        assertTrue(cbzExtractor is CbzCbrBookExtractor)

        val docxExtractor = BookExtractorFactory.getExtractor("test.docx")
        assertNotNull(docxExtractor)
        assertTrue(docxExtractor is DocxBookExtractor)

        val odtExtractor = BookExtractorFactory.getExtractor("test.odt")
        assertNotNull(odtExtractor)
        assertTrue(odtExtractor is OdtBookExtractor)

        val mdExtractor = BookExtractorFactory.getExtractor("test.md")
        assertNotNull(mdExtractor)
        assertTrue(mdExtractor is MarkdownBookExtractor)

        val djvuExtractor = BookExtractorFactory.getExtractor("test.djvu")
        assertNotNull(djvuExtractor)
        assertTrue(djvuExtractor is DjvuBookExtractor)
    }

    @Test
    fun testGetExtractorForUnsupportedFormats() {
        val unknownExtractor = BookExtractorFactory.getExtractor("test.unknown")
        assertNull(unknownExtractor)
    }
}
