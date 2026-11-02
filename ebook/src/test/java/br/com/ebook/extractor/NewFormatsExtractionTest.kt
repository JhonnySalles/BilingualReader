package br.com.ebook.extractor

import br.com.ebook.core.BookContent
import br.com.ebook.core.BookExtractorFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.zip.ZipFile

class NewFormatsExtractionTest {

    private val testDir = sequenceOf(File("../teste"), File("teste"), File("f:/Projetos/BilingualReader/teste"))
        .firstOrNull { it.exists() && it.isDirectory } ?: File("../teste")

    @Test
    fun testBookExtractorFactoryRouting() {
        assertTrue(BookExtractorFactory.getExtractor("book.htmlz") is HtmlzBookExtractor)
        assertTrue(BookExtractorFactory.getExtractor("book.txtz") is TxtzBookExtractor)
        assertTrue(BookExtractorFactory.getExtractor("book.rb") is RbBookExtractor)
        assertTrue(BookExtractorFactory.getExtractor("book.snb") is SnbBookExtractor)
        assertTrue(BookExtractorFactory.getExtractor("book.doc") is DocBookExtractor)
        assertTrue(BookExtractorFactory.getExtractor("book.docx") is DocxBookExtractor)
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

        // 5. Content
        val tempDir = File(System.getProperty("java.io.tmpdir"), "htmlz_unit_test")
        val content = HtmlzBookExtractor.extractContent(file.absolutePath, tempDir.absolutePath).getOrThrow()
        assertTrue(content is BookContent.EpubFile, "HTMLZ content should be EpubFile")
        val epubFile = File((content as BookContent.EpubFile).path)
        assertTrue(epubFile.exists() && epubFile.length() > 0, "HTMLZ EPUB file should exist and not be empty")

        ZipFile(epubFile).use { zip ->
            // Must have cover.xhtml as the first spine item
            val opfEntry = zip.getEntry("OEBPS/content.opf")
            assertNotNull(opfEntry)
            val opfText = zip.getInputStream(opfEntry).bufferedReader(Charsets.UTF_8).readText()
            assertTrue(opfText.contains("<itemref idref=\"cover-page\" />"), "Cover must be in spine")

            // Count full-page image chapters (<div class="br-full-page">)
            var fullPageImgCount = 0
            if (zip.getEntry("OEBPS/cover.xhtml") != null) {
                fullPageImgCount++ // Cover counts as image page 1
            }

            for (entry in zip.entries().asSequence()) {
                if (entry.name.startsWith("OEBPS/") && entry.name.endsWith(".xhtml") && !entry.name.endsWith("cover.xhtml")) {
                    val html = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
                    if (html.contains("class=\"br-full-page\"")) {
                        fullPageImgCount++
                    }
                }
            }

            // Must have exactly 15 full-page images (1 cover + 14 full-page illustrations)
            assertEquals(15, fullPageImgCount, "HTMLZ must generate exactly 15 full-page image items")

            // Gallery items must be empty
            val galleryEntry = zip.getEntry("OEBPS/gallery_0.xhtml")
            assertTrue(galleryEntry == null, "HTMLZ must not have gallery items")

            // Base CSS and original style.css must exist
            assertNotNull(zip.getEntry("OEBPS/reader-base.css"), "reader-base.css must exist")
            assertNotNull(zip.getEntry("OEBPS/style.css"), "Original style.css must be preserved")
        }
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

        // 3. Overview
        val overview = TxtzBookExtractor.extractOverview(file.absolutePath).getOrThrow()
        assertNotNull(overview)

        // 4. Content
        val tempDir = File(System.getProperty("java.io.tmpdir"), "txtz_unit_test")
        val content = TxtzBookExtractor.extractContent(file.absolutePath, tempDir.absolutePath).getOrThrow()
        assertTrue(content is BookContent.EpubFile, "TXTZ content should be EpubFile")
        val epubFile = File((content as BookContent.EpubFile).path)
        assertTrue(epubFile.exists() && epubFile.length() > 0, "TXTZ EPUB file should exist and not be empty")

        ZipFile(epubFile).use { zip ->
            // Cover in spine
            assertNotNull(zip.getEntry("OEBPS/cover.xhtml"))

            // Chap 0 must contain TOC lines without breaking into 1-line chapters
            val chap0Entry = zip.getEntry("OEBPS/chap_0.xhtml")
            assertNotNull(chap0Entry)
            val chap0Text = zip.getInputStream(chap0Entry).bufferedReader(Charsets.UTF_8).readText()
            assertTrue(chap0Text.length > 500, "chap_0.xhtml must contain the opening and TOC, length: ${chap0Text.length}")
            assertTrue(chap0Text.contains("AfterWord"), "chap_0.xhtml must contain TOC entries including AfterWord")

            // Real chapter 1 starts in chap_1.xhtml
            val chap1Entry = zip.getEntry("OEBPS/chap_1.xhtml")
            assertNotNull(chap1Entry)
            val chap1Text = zip.getInputStream(chap1Entry).bufferedReader(Charsets.UTF_8).readText()
            assertTrue(chap1Text.contains("CAP"), "chap_1.xhtml must start with Chapter 1")

            // Gallery items: cover.jpg is identical to cover.jpeg, so it must NOT be duplicated in gallery
            val galleryEntry = zip.getEntry("OEBPS/gallery_0.xhtml")
            assertNull(galleryEntry, "TXTZ gallery must not duplicate cover image in 01-86")
        }

        // 5. Outline (10 chapters, no gallery duplicate)
        val outline = TxtzBookExtractor.extractOutline(file.absolutePath)
        assertEquals(10, outline.size, "TXTZ outline should have exactly 10 real chapters with no duplicate cover gallery")
        assertTrue(outline[0].link.startsWith("chap_1.xhtml#"), "First outline chapter should point to chap_1.xhtml")
    }

    @Test
    fun testRbExtraction() = runBlocking {
        val file = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.rb")
        if (!file.exists()) return@runBlocking

        // 1. Metadata
        val meta = RbBookExtractor.extractMetadata(file.absolutePath).getOrThrow()
        assertNotNull(meta)
        assertTrue(meta.title.contains("86", ignoreCase = true), "Title should contain 86: ${meta.title}")

        // 2. Cover: primeira imagem (12.png de ~995 KB)
        val cover = RbBookExtractor.extractCover(file.absolutePath).getOrThrow()
        assertNotNull(cover, "Cover should not be null")
        assertTrue(cover!!.isNotEmpty(), "Cover should not be empty")
        assertTrue(cover!!.size > 900000, "Cover should be 12.png (~995 KB), found size: ${cover!!.size}")

        // 3. Content
        val tempDir = File(System.getProperty("java.io.tmpdir"), "rb_unit_test")
        val content = RbBookExtractor.extractContent(file.absolutePath, tempDir.absolutePath).getOrThrow()
        assertTrue(content is BookContent.EpubFile, "RB content should be EpubFile")
        val epubFile = File((content as BookContent.EpubFile).path)
        assertTrue(epubFile.exists() && epubFile.length() > 0, "RB EPUB file should exist and not be empty")

        ZipFile(epubFile).use { zip ->
            // Cover
            assertNotNull(zip.getEntry("OEBPS/cover.xhtml"))

            var fullPageImgCount = 1 // cover.xhtml is 1
            for (entry in zip.entries().asSequence()) {
                if (entry.name.startsWith("OEBPS/") && entry.name.endsWith(".xhtml") && !entry.name.endsWith("cover.xhtml")) {
                    val html = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
                    if (html.contains("class=\"br-full-page\"")) {
                        fullPageImgCount++
                    }
                }
            }

            assertEquals(15, fullPageImgCount, "RB must generate exactly 15 full-page image items")

            // Gallery items must be empty
            val galleryEntry = zip.getEntry("OEBPS/gallery_0.xhtml")
            assertTrue(galleryEntry == null, "RB must not have gallery items")

            // Check accents
            val allXhtml = zip.entries().asSequence().filter { it.name.endsWith(".xhtml") }
                .map { zip.getInputStream(it).bufferedReader(Charsets.UTF_8).readText() }
                .joinToString("\n")
            assertTrue(allXhtml.contains("Tradução"), "Accents should be preserved in RB")
        }
    }

    @Test
    fun testDocxExtraction() = runBlocking {
        val file = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.docx")
        if (!file.exists()) return@runBlocking

        // 1. Metadata
        val meta = DocxBookExtractor.extractMetadata(file.absolutePath).getOrThrow()
        assertNotNull(meta)
        assertTrue(meta.title.isNotBlank(), "Title should not be blank")

        // 2. Cover (media/cover.jpeg ~468 KB)
        val cover = DocxBookExtractor.extractCover(file.absolutePath).getOrThrow()
        assertNotNull(cover, "DOCX cover should not be null")
        assertTrue(cover!!.isNotEmpty(), "DOCX cover should not be empty")
        assertTrue(cover!!.size > 400000, "DOCX cover should be media/cover.jpeg (~468 KB), found size: ${cover!!.size}")

        // 3. Content
        val tempDir = File(System.getProperty("java.io.tmpdir"), "docx_unit_test")
        val content = DocxBookExtractor.extractContent(file.absolutePath, tempDir.absolutePath).getOrThrow()
        assertTrue(content is BookContent.EpubFile, "DOCX content should be EpubFile")
        val epubFile = File((content as BookContent.EpubFile).path)
        assertTrue(epubFile.exists() && epubFile.length() > 0, "DOCX EPUB file should exist and not be empty")

        ZipFile(epubFile).use { zip ->
            // Cover
            assertNotNull(zip.getEntry("OEBPS/cover.xhtml"))

            var fullPageImgCount = 1 // cover.xhtml is 1
            for (entry in zip.entries().asSequence()) {
                if (entry.name.startsWith("OEBPS/") && entry.name.endsWith(".xhtml") && !entry.name.endsWith("cover.xhtml")) {
                    val html = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
                    if (html.contains("class=\"br-full-page\"")) {
                        fullPageImgCount++
                    }
                }
            }

            assertEquals(15, fullPageImgCount, "DOCX must generate exactly 15 full-page image items")

            // Gallery items must be empty (no duplicates!)
            val galleryEntry = zip.getEntry("OEBPS/gallery_0.xhtml")
            assertTrue(galleryEntry == null, "DOCX must not have duplicate gallery items")

            // Verify NO chapter in the EPUB is empty (every chapter must have visible text or an image)
            for (entry in zip.entries().asSequence()) {
                if (entry.name.startsWith("OEBPS/") && entry.name.endsWith(".xhtml")) {
                    val html = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
                    val doc = org.jsoup.Jsoup.parse(html)
                    val hasText = doc.text().trim().isNotEmpty()
                    val hasImg = doc.select("img, image").isNotEmpty()
                    assertTrue(hasText || hasImg, "DOCX Chapter ${entry.name} must not be blank!")
                }
            }
        }
    }

    @Test
    fun testSnbExtraction() = runBlocking {
        val file = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.snb")
        if (!file.exists()) return@runBlocking

        val meta = SnbBookExtractor.extractMetadata(file.absolutePath).getOrThrow()
        assertNotNull(meta)
        assertTrue(meta.title.contains("86"), "SNB title should contain 86")

        val cover = SnbBookExtractor.extractCover(file.absolutePath).getOrThrow()
        assertNotNull(cover)
        assertTrue(cover!!.isNotEmpty())

        val tempDir = File(System.getProperty("java.io.tmpdir"), "snb_unit_test")
        val content = SnbBookExtractor.extractContent(file.absolutePath, tempDir.absolutePath).getOrThrow()
        assertTrue(content is BookContent.EpubFile, "SNB content should be EpubFile")
        val epubFile = File((content as BookContent.EpubFile).path)
        assertTrue(epubFile.exists() && epubFile.length() > 0)

        ZipFile(epubFile).use { zip ->
            // Cover
            assertNotNull(zip.getEntry("OEBPS/cover.xhtml"))

            // Spine items immediately following cover must be the illustration pages (Folha de rosto + 3 inserts)
            assertNotNull(zip.getEntry("OEBPS/img_page_1_0.xhtml"), "Folha de rosto must exist right after cover")
            assertNotNull(zip.getEntry("OEBPS/img_page_2_0.xhtml"), "Insert 2 must exist")
            assertNotNull(zip.getEntry("OEBPS/img_page_2_1.xhtml"), "Insert 3 must exist")
            assertNotNull(zip.getEntry("OEBPS/img_page_2_2.xhtml"), "Insert 4 must exist")

            // Next is copyright text
            assertNotNull(zip.getEntry("OEBPS/chap_3.xhtml"), "Chapter 3 (copyright) must follow inserts")

            // Gallery items must be empty because all illustrations are in sequential order
            val galleryEntry = zip.getEntry("OEBPS/gallery_0.xhtml")
            assertTrue(galleryEntry == null, "SNB illustrations must be in linear order, not in gallery")
        }
    }

    @Test
    fun testTxtzWithGalleryExtraction() = runBlocking {
        // Create mock TXTZ with index.txt, cover.jpg, and an extra illustration
        val tempTxtz = File(System.getProperty("java.io.tmpdir"), "mock_gallery.txtz")
        val fakeJpeg = ByteArray(100) { 0x11.toByte() }
        fakeJpeg[0] = 0xFF.toByte(); fakeJpeg[1] = 0xD8.toByte(); fakeJpeg[fakeJpeg.size - 2] = 0xFF.toByte(); fakeJpeg[fakeJpeg.size - 1] = 0xD9.toByte()
        val fakePng = ByteArray(100) { 0x22.toByte() }

        java.util.zip.ZipOutputStream(java.io.FileOutputStream(tempTxtz)).use { zos ->
            zos.putNextEntry(java.util.zip.ZipEntry("index.txt"))
            zos.write("Capítulo 1\nEste é o texto do livro de teste.".toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(java.util.zip.ZipEntry("cover.jpg"))
            zos.write(fakeJpeg)
            zos.closeEntry()

            // Duplicate cover under another name
            zos.putNextEntry(java.util.zip.ZipEntry("cover.jpeg"))
            zos.write(fakeJpeg)
            zos.closeEntry()

            zos.putNextEntry(java.util.zip.ZipEntry("illustration_01.png"))
            zos.write(fakePng)
            zos.closeEntry()
        }

        val tempDir = File(System.getProperty("java.io.tmpdir"), "txtz_mock_gallery_test")
        val content = TxtzBookExtractor.extractContent(tempTxtz.absolutePath, tempDir.absolutePath).getOrThrow()
        assertTrue(content is BookContent.EpubFile)
        val epubFile = File((content as BookContent.EpubFile).path)

        ZipFile(epubFile).use { zip ->
            assertNotNull(zip.getEntry("OEBPS/cover.xhtml"))
            // Gallery item must be present at the end ONLY for illustration_01.png (cover.jpeg must be excluded)
            val galleryEntry = zip.getEntry("OEBPS/gallery_0.xhtml")
            assertNotNull(galleryEntry, "Gallery item must exist for illustration_01.png")
            val galleryHtml = zip.getInputStream(galleryEntry).bufferedReader(Charsets.UTF_8).readText()
            assertTrue(galleryHtml.contains("illustration_01.png"))

            val galleryEntry2 = zip.getEntry("OEBPS/gallery_1.xhtml")
            assertNull(galleryEntry2, "Duplicate cover.jpeg must not generate gallery_1.xhtml")
        }

        val outline = TxtzBookExtractor.extractOutline(tempTxtz.absolutePath)
        val galleryOutline = outline.filter { it.title.contains("Galeria") }
        assertEquals(1, galleryOutline.size, "Outline should include exactly 1 gallery entry for illustration_01")
    }

    @Test
    fun testDocExtraction() = runBlocking {
        // Create a simple mock .doc file with text and JPEG image
        val tempDoc = File(System.getProperty("java.io.tmpdir"), "test_sample.doc")
        val fakeJpeg = ByteArray(3000) { 0 }
        fakeJpeg[0] = 0xFF.toByte(); fakeJpeg[1] = 0xD8.toByte(); fakeJpeg[2] = 0xFF.toByte()
        fakeJpeg[fakeJpeg.size - 2] = 0xFF.toByte(); fakeJpeg[fakeJpeg.size - 1] = 0xD9.toByte()
        // SOF0 marker for 1200x1800
        fakeJpeg[3] = 0xFF.toByte(); fakeJpeg[4] = 0xC0.toByte(); fakeJpeg[5] = 0x00.toByte(); fakeJpeg[6] = 0x11.toByte()
        fakeJpeg[7] = 0x08.toByte()
        fakeJpeg[8] = 0x07.toByte(); fakeJpeg[9] = 0x08.toByte() // height 1800
        fakeJpeg[10] = 0x04.toByte(); fakeJpeg[11] = 0xB0.toByte() // width 1200

        val textPart = "Hello World! This is a test document with enough text to verify .doc extraction.".toByteArray(Charsets.US_ASCII)
        tempDoc.writeBytes(fakeJpeg + textPart)

        val tempDir = File(System.getProperty("java.io.tmpdir"), "doc_unit_test")
        val content = DocBookExtractor.extractContent(tempDoc.absolutePath, tempDir.absolutePath).getOrThrow()
        assertTrue(content is BookContent.EpubFile, "DOC content should be EpubFile")
        val epubFile = File((content as BookContent.EpubFile).path)
        assertTrue(epubFile.exists() && epubFile.length() > 0, "DOC EPUB file should exist")
    }
}
