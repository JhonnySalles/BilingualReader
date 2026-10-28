package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractorFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class NewFormatsExtractionTest {

    private val testDir = sequenceOf(File("../teste"), File("teste"), File("f:/Projetos/BilingualReader/teste"))
        .firstOrNull { it.exists() && it.isDirectory } ?: File("../teste")

    @Test
    fun testBookExtractorFactoryRouting() {
        assertTrue(BookExtractorFactory.getExtractor("book.htmlz") is HtmlzBookExtractor)
        assertTrue(BookExtractorFactory.getExtractor("book.txtz") is TxtzBookExtractor)
        assertTrue(BookExtractorFactory.getExtractor("book.rb") is RbBookExtractor)
        assertTrue(BookExtractorFactory.getExtractor("book.snb") is SnbBookExtractor)
    }

    @Test
    fun testHtmlzExtraction() = runBlocking {
        val file = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.htmlz")
        if (!file.exists()) return@runBlocking

        // 1. Metadata
        val meta = HtmlzBookExtractor.extractMetadata(file.absolutePath).getOrThrow()
        assertNotNull(meta)
        assertTrue(meta.title.contains("86", ignoreCase = true), "Title should contain 86: ${meta.title}")
        assertTrue(meta.author.isNotBlank(), "Author should not be blank")

        // 2. Cover
        val cover = HtmlzBookExtractor.extractCover(file.absolutePath).getOrThrow()
        assertNotNull(cover, "Cover bytes should not be null")
        assertTrue(cover!!.isNotEmpty(), "Cover bytes should not be empty")

        // 3. Overview
        val overview = HtmlzBookExtractor.extractOverview(file.absolutePath).getOrThrow()
        assertTrue(overview.isNotBlank(), "Overview should not be blank")

        // 4. Outline / Chapters
        val outline = HtmlzBookExtractor.extractOutline(file.absolutePath)
        assertTrue(outline.isNotEmpty(), "Outline should have chapters")
        assertTrue(outline.size >= 10, "Outline should have at least 10 chapters, found ${outline.size}")

        // 5. Content
        val tempDir = File(System.getProperty("java.io.tmpdir"), "htmlz_unit_test")
        val content = HtmlzBookExtractor.extractContent(file.absolutePath, tempDir.absolutePath).getOrThrow()
        assertTrue(content is BookContent.HtmlFile)
        assertTrue(File((content as BookContent.HtmlFile).path).exists())
    }

    @Test
    fun testTxtzExtraction() = runBlocking {
        val file = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.txtz")
        if (!file.exists()) return@runBlocking

        // 1. Metadata
        val meta = TxtzBookExtractor.extractMetadata(file.absolutePath).getOrThrow()
        assertNotNull(meta)
        assertTrue(meta.title.isNotBlank(), "Title should not be blank")

        // 2. Cover
        val cover = TxtzBookExtractor.extractCover(file.absolutePath).getOrThrow()
        assertNotNull(cover, "Cover bytes should not be null")
        assertTrue(cover!!.isNotEmpty(), "Cover bytes should not be empty")

        // 3. Overview (txtz sample does not have dc:description in opf, but should not throw)
        val overview = TxtzBookExtractor.extractOverview(file.absolutePath).getOrThrow()
        assertNotNull(overview)

        // 4. Content
        val tempDir = File(System.getProperty("java.io.tmpdir"), "txtz_unit_test")
        val content = TxtzBookExtractor.extractContent(file.absolutePath, tempDir.absolutePath).getOrThrow()
        assertTrue(content is BookContent.HtmlFile)
        assertTrue(File((content as BookContent.HtmlFile).path).exists())
    }

    @Test
    fun testRbExtraction() = runBlocking {
        val file = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.rb")
        if (!file.exists()) return@runBlocking

        // 1. Metadata
        val meta = RbBookExtractor.extractMetadata(file.absolutePath).getOrThrow()
        assertNotNull(meta)
        assertTrue(meta.title.contains("86", ignoreCase = true), "Title should contain 86: ${meta.title}")

        // 2. Cover
        val cover = RbBookExtractor.extractCover(file.absolutePath).getOrThrow()
        assertNotNull(cover, "Cover should not be null")
        assertTrue(cover!!.isNotEmpty(), "Cover should not be empty")

        // 3. Content
        val tempDir = File(System.getProperty("java.io.tmpdir"), "rb_unit_test")
        val content = RbBookExtractor.extractContent(file.absolutePath, tempDir.absolutePath).getOrThrow()
        assertTrue(content is BookContent.HtmlFile)
        assertTrue(File((content as BookContent.HtmlFile).path).exists())
    }

    @Test
    fun testSnbExtraction() = runBlocking {
        val file = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.snb")
        if (!file.exists()) return@runBlocking

        // 1. Metadata
        val meta = SnbBookExtractor.extractMetadata(file.absolutePath).getOrThrow()
        assertNotNull(meta)
        assertTrue(meta.title.contains("86", ignoreCase = true), "Title should contain 86: ${meta.title}")
        assertTrue(meta.author.isNotBlank(), "Author should not be blank")

        // 2. Cover
        val cover = SnbBookExtractor.extractCover(file.absolutePath).getOrThrow()
        assertNotNull(cover, "Cover should not be null")
        assertTrue(cover!!.isNotEmpty(), "Cover should not be empty")

        // 3. Overview
        val overview = SnbBookExtractor.extractOverview(file.absolutePath).getOrThrow()
        assertTrue(overview.isNotBlank(), "Overview should not be blank")

        // 4. Outline / Chapters
        val outline = SnbBookExtractor.extractOutline(file.absolutePath)
        assertTrue(outline.isNotEmpty(), "Outline should have chapters")
        assertTrue(outline.size >= 10, "Outline should have at least 10 chapters, found ${outline.size}")

        // 5. Content
        val tempDir = File(System.getProperty("java.io.tmpdir"), "snb_unit_test")
        val content = SnbBookExtractor.extractContent(file.absolutePath, tempDir.absolutePath).getOrThrow()
        assertTrue(content is BookContent.HtmlFile)
        assertTrue(File((content as BookContent.HtmlFile).path).exists())
    }
}
