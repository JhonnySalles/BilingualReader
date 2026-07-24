package br.com.ebook.extractor

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.File

class EpubTocExtractorTest {

    @Test
    fun testExtractTocOutlineFromTestFiles() {
        val filesDir = File("../files")
        if (!filesDir.exists() || !filesDir.isDirectory) return

        val epubs = filesDir.listFiles { _, name -> name.endsWith(".epub") } ?: return
        for (epub in epubs) {
            val toc = EpubBookExtractor.extractTocOutline(epub.absolutePath)
            assertNotNull(toc, "TOC outline should not be null for ${epub.name}")
            assertFalse(toc.isEmpty(), "TOC outline should contain chapters for ${epub.name}")
        }
    }
}
