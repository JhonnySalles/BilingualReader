package br.com.ebook.extractor

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class FormatChapterExtractionTest {

    private val testDir = File("../teste")

    @Test
    fun testKepubChapterExtraction() {
        val kepubFile = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.kepub.epub")
        if (!kepubFile.exists()) return

        val outline = EpubBookExtractor.extractTocOutline(kepubFile.absolutePath)
        assertNotNull(outline, "KEPUB outline should not be null")
        assertTrue(outline.isNotEmpty(), "KEPUB outline should have chapters")
        assertTrue(outline.size >= 10, "Should have at least 10 chapters in KEPUB")
    }

    @Test
    fun testFb2ChapterExtraction() {
        val fb2File = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.fb2")
        if (!fb2File.exists()) return

        val outline = Fb2BookExtractor.extractOutline(fb2File.absolutePath)
        assertNotNull(outline, "FB2 outline should not be null")
        assertTrue(outline.isNotEmpty(), "FB2 outline should have chapters")
        assertTrue(outline.size >= 10, "Should have at least 10 chapters in FB2")
    }

    @Test
    fun testDocxChapterExtraction() {
        val docxFile = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.docx")
        if (!docxFile.exists()) return

        val outline = DocxBookExtractor.extractOutline(docxFile.absolutePath)
        assertNotNull(outline, "DOCX outline should not be null")
        assertTrue(outline.isNotEmpty(), "DOCX outline should have chapters")
        assertTrue(outline.size >= 10, "Should have at least 10 chapters in DOCX")
    }
}
