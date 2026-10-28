package br.com.ebook.extractor

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class EpubMetadataExtractorTest {

    @Test
    fun testExtractMetadataFromIrregularNovel() {
        val file = File("../teste/The Irregular at Magic High School - Volume 23 (Eng).epub")
        if (!file.exists()) {
            val alt = File("teste/The Irregular at Magic High School - Volume 23 (Eng).epub")
            if (!alt.exists()) return
        }
        val targetFile = if (file.exists()) file else File("teste/The Irregular at Magic High School - Volume 23 (Eng).epub")

        val metadataResult = runBlocking {
            EpubBookExtractor.extractMetadata(targetFile.absolutePath)
        }

        if (metadataResult.isFailure) {
            val ex = metadataResult.exceptionOrNull()
            println("FAILURE EXCEPTION: ${ex?.message}")
            ex?.printStackTrace()
        }
        val metadata = metadataResult.getOrThrow()

        assertTrue(metadata.title.contains("The Irregular at Magic High School"), "Title should match")
        assertTrue(metadata.author.contains("Tsutomu Sato"), "Author should contain Tsutomu Sato")
        assertEquals("The Irregular at Magic High School", metadata.series, "Series should match belongs-to-collection")
        assertEquals(23, metadata.seriesIndex, "Series index/volume should match group-position 23")
        assertFalse(metadata.annotation.isBlank(), "Annotation/description should not be blank")
        assertTrue(metadata.annotation.contains("Late April 2097"), "Annotation should contain book description text")

        val toc = EpubBookExtractor.extractTocOutline(targetFile.absolutePath)
        assertNotNull(toc, "TOC outline should not be null")
        assertTrue(toc.size >= 10, "TOC should contain multiple chapters (found: ${toc.size})")
        assertTrue(toc.any { it.title.contains("Chapter 1") }, "TOC should contain Chapter 1")
    }
}
