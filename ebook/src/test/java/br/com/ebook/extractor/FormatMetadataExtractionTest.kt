package br.com.ebook.extractor

import br.com.ebook.core.BookExtractorFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class FormatMetadataExtractionTest {

    private val testDir = sequenceOf(
        File("../Teste"),
        File("Teste"),
        File("f:/Projetos/BilingualReader/Teste"),
        File("../teste"),
        File("teste"),
        File("f:/Projetos/BilingualReader/teste")
    ).firstOrNull { it.exists() && it.isDirectory } ?: File("../Teste")

    @Test
    fun testEpubMetadata() = runBlocking {
        val file = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.epub")
        if (!file.exists()) return@runBlocking

        val meta = BookExtractorFactory.getMetadata(file.absolutePath)
        assertTrue(meta.title.contains("86--EIGHTY-SIX"), "Title: ${meta.title}")
        assertTrue(meta.author.contains("Asato") && meta.author.contains("Shirabii"), "Author: ${meta.author}")
        assertTrue(meta.series.contains("86-EIGHTY-SIX"), "Series: ${meta.series}")
        assertEquals(1, meta.seriesIndex, "SeriesIndex: ${meta.seriesIndex}")
        assertEquals("Yen On", meta.publisher, "Publisher: ${meta.publisher}")
        assertEquals("9781975303129", meta.isbn, "ISBN: ${meta.isbn}")
        assertNotNull(meta.releaseDate, "ReleaseDate should not be null")
        assertTrue(meta.genre.contains("Fiction"), "Genre: ${meta.genre}")
        assertTrue(meta.annotation.isNotBlank(), "Annotation should not be blank")
        assertFalse(meta.annotation.contains("<div") || meta.annotation.contains("<p>"), "Annotation must not contain raw HTML")
    }

    @Test
    fun testKepubMetadata() = runBlocking {
        val file = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.kepub")
        if (!file.exists()) return@runBlocking

        val meta = BookExtractorFactory.getMetadata(file.absolutePath)
        assertTrue(meta.title.contains("86--EIGHTY-SIX"), "Title: ${meta.title}")
        assertTrue(meta.author.contains("Asato"), "Author: ${meta.author}")
        assertEquals("Yen On", meta.publisher, "Publisher: ${meta.publisher}")
        assertEquals("9781975303129", meta.isbn, "ISBN: ${meta.isbn}")
    }

    @Test
    fun testHtmlzMetadata() = runBlocking {
        val file = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.htmlz")
        if (!file.exists()) return@runBlocking

        val meta = BookExtractorFactory.getMetadata(file.absolutePath)
        assertTrue(meta.title.contains("86--EIGHTY-SIX"), "Title: ${meta.title}")
        assertTrue(meta.author.contains("Asato") && meta.author.contains("Shirabii"), "Author: ${meta.author}")
        assertTrue(meta.series.contains("86-EIGHTY-SIX"), "Series: ${meta.series}")
        assertEquals(1, meta.seriesIndex, "SeriesIndex: ${meta.seriesIndex}")
        assertEquals("Yen On", meta.publisher, "Publisher: ${meta.publisher}")
        assertEquals("9781975303129", meta.isbn, "ISBN: ${meta.isbn}")
        assertNotNull(meta.releaseDate, "ReleaseDate should not be null")
        assertTrue(meta.genre.contains("Fiction"), "Genre: ${meta.genre}")
        assertTrue(meta.annotation.isNotBlank(), "Annotation should not be blank")
        assertFalse(meta.annotation.contains("<div") || meta.annotation.contains("<p>"), "Annotation must not contain raw HTML")
    }

    @Test
    fun testTxtzMetadata() = runBlocking {
        val file = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.txtz")
        if (!file.exists()) return@runBlocking

        val meta = BookExtractorFactory.getMetadata(file.absolutePath)
        assertTrue(meta.title.contains("86--EIGHTY-SIX"), "Title: ${meta.title}")
        assertTrue(meta.author.contains("Asato") && meta.author.contains("Shirabii"), "Author: ${meta.author}")
        assertTrue(meta.series.contains("86-EIGHTY-SIX"), "Series: ${meta.series}")
        assertEquals(1, meta.seriesIndex, "SeriesIndex: ${meta.seriesIndex}")
        assertEquals("Yen On", meta.publisher, "Publisher: ${meta.publisher}")
        assertEquals("9781975303129", meta.isbn, "ISBN: ${meta.isbn}")
        assertNotNull(meta.releaseDate, "ReleaseDate should not be null")
        assertTrue(meta.genre.contains("Fiction"), "Genre: ${meta.genre}")
        assertTrue(meta.annotation.isNotBlank(), "Annotation should not be blank")
        assertFalse(meta.annotation.contains("<div") || meta.annotation.contains("<p>"), "Annotation must not contain raw HTML")
    }

    @Test
    fun testDocxMetadata() = runBlocking {
        val file = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.docx")
        if (!file.exists()) return@runBlocking

        val meta = BookExtractorFactory.getMetadata(file.absolutePath)
        assertTrue(meta.title.contains("86--EIGHTY-SIX"), "Title: ${meta.title}")
        assertTrue(meta.author.contains("Asato") && meta.author.contains("Shirabii"), "Author: ${meta.author}")
        assertEquals("Yen On", meta.publisher, "Publisher should come from app.xml Company: ${meta.publisher}")
        assertTrue(meta.series.contains("86"), "Series: ${meta.series}")
        assertEquals(1, meta.seriesIndex, "SeriesIndex: ${meta.seriesIndex}")
        assertNotNull(meta.releaseDate, "ReleaseDate should come from dcterms:created")
        assertTrue(meta.genre.contains("Fiction"), "Genre: ${meta.genre}")
        assertTrue(meta.annotation.isNotBlank(), "Annotation should not be blank")
    }

    @Test
    fun testFb2Metadata() = runBlocking {
        val file = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.fb2")
        if (!file.exists()) return@runBlocking

        val meta = BookExtractorFactory.getMetadata(file.absolutePath)
        assertTrue(meta.title.contains("86--EIGHTY-SIX"), "Title: ${meta.title}")
        assertTrue(meta.author.contains("Asato") && meta.author.contains("Shirabii"), "Author: ${meta.author}")
        assertTrue(meta.series.contains("86-EIGHTY-SIX"), "Series: ${meta.series}")
        assertEquals(1, meta.seriesIndex, "SeriesIndex: ${meta.seriesIndex}")
        assertEquals("Yen On", meta.publisher, "Publisher: ${meta.publisher}")
        assertEquals("9781975303129", meta.isbn, "ISBN: ${meta.isbn}")
        assertNotNull(meta.releaseDate, "ReleaseDate should not be null")
        assertTrue(meta.genre.contains("Fiction") || meta.genre.contains("Traduzido"), "Genre: ${meta.genre}")
        assertTrue(meta.annotation.isNotBlank(), "Annotation should not be blank")
    }

    @Test
    fun testMobiAndAzw3Metadata() = runBlocking {
        val mobiFile = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.mobi")
        if (mobiFile.exists()) {
            val meta = BookExtractorFactory.getMetadata(mobiFile.absolutePath)
            assertTrue(meta.title.contains("86--EIGHTY-SIX"), "Title: ${meta.title}")
            assertTrue(meta.author.contains("Asato") && meta.author.contains("Shirabii"), "Author: ${meta.author}")
            assertEquals("Yen On", meta.publisher, "Publisher: ${meta.publisher}")
            assertEquals("9781975303129", meta.isbn, "ISBN: ${meta.isbn}")
            assertTrue(meta.series.contains("86"), "Series: ${meta.series}")
            assertEquals(1, meta.seriesIndex, "SeriesIndex: ${meta.seriesIndex}")
            assertTrue(meta.annotation.isNotBlank(), "Annotation: ${meta.annotation}")
            assertFalse(meta.annotation.contains("<div") || meta.annotation.contains("<p>"), "Annotation must not contain raw HTML")
        }

        val azw3File = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.azw3")
        if (azw3File.exists()) {
            val meta = BookExtractorFactory.getMetadata(azw3File.absolutePath)
            assertTrue(meta.title.contains("86--EIGHTY-SIX"), "Title: ${meta.title}")
            assertTrue(meta.author.contains("Asato") && meta.author.contains("Shirabii"), "Author: ${meta.author}")
            assertEquals("Yen On", meta.publisher, "Publisher: ${meta.publisher}")
            assertEquals("9781975303129", meta.isbn, "ISBN: ${meta.isbn}")
            assertEquals(1, meta.seriesIndex, "SeriesIndex: ${meta.seriesIndex}")
        }
    }

    @Test
    fun testPdfMetadata() = runBlocking {
        val file = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.pdf")
        if (!file.exists()) return@runBlocking

        val meta = BookExtractorFactory.getMetadata(file.absolutePath, withPDF = true)
        assertTrue(meta.title.contains("86--EIGHTY-SIX"), "Title: ${meta.title}")
        assertTrue(meta.author.contains("Asato") && meta.author.contains("Shirabii"), "Author: ${meta.author}")
        assertTrue(meta.series.contains("86-EIGHTY-SIX"), "Series: ${meta.series}")
        assertEquals(1, meta.seriesIndex, "SeriesIndex: ${meta.seriesIndex}")
        assertEquals("Yen On", meta.publisher, "Publisher: ${meta.publisher}")
        assertEquals("9781975303129", meta.isbn, "ISBN: ${meta.isbn}")
        assertNotNull(meta.releaseDate, "ReleaseDate: ${meta.releaseDate}")
        assertTrue(meta.genre.contains("Fiction"), "Genre: ${meta.genre}")
        assertTrue(meta.annotation.isNotBlank(), "Annotation: ${meta.annotation}")
    }

    @Test
    fun testSnbMetadata() = runBlocking {
        val file = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.snb")
        if (!file.exists()) return@runBlocking

        val meta = BookExtractorFactory.getMetadata(file.absolutePath)
        assertTrue(meta.title.contains("86--EIGHTY-SIX"), "Title: ${meta.title}")
        assertTrue(meta.author.contains("Asato"), "Author: ${meta.author}")
        assertEquals("Yen On", meta.publisher, "Publisher: ${meta.publisher}")
        assertTrue(meta.series.contains("86"), "Series: ${meta.series}")
        assertEquals(1, meta.seriesIndex, "SeriesIndex: ${meta.seriesIndex}")
        assertEquals("POR", meta.language.uppercase(), "Language: ${meta.language}")
        assertTrue(meta.annotation.isNotBlank(), "Annotation: ${meta.annotation}")
        assertFalse(meta.annotation.contains("<div") || meta.annotation.contains("<p>"), "Annotation must not contain raw HTML")
    }

    @Test
    fun testRbMetadata() = runBlocking {
        val file = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.rb")
        if (!file.exists()) return@runBlocking

        val meta = BookExtractorFactory.getMetadata(file.absolutePath)
        assertTrue(meta.title.contains("86--EIGHTY-SIX"), "Title: ${meta.title}")
        assertTrue(meta.author.contains("Asato") && meta.author.contains("Shirabii"), "Author: ${meta.author}")
        assertEquals("Yen On", meta.publisher, "Publisher: ${meta.publisher}")
        assertEquals(1, meta.seriesIndex, "SeriesIndex: ${meta.seriesIndex}")
        assertNotNull(meta.releaseDate, "ReleaseDate: ${meta.releaseDate}")
    }

    @Test
    fun testPdbAndTcrMetadata() = runBlocking {
        val pdbFile = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.pdb")
        if (pdbFile.exists()) {
            val meta = BookExtractorFactory.getMetadata(pdbFile.absolutePath)
            assertTrue(meta.title.contains("86--EIGHTY-SIX"), "Title: ${meta.title}")
            assertFalse(meta.title.contains("_ Vol_ "), "Title should not have underscores from Palm header: ${meta.title}")
            assertEquals("Yen On", meta.publisher, "Publisher: ${meta.publisher}")
            assertEquals(1, meta.seriesIndex, "SeriesIndex: ${meta.seriesIndex}")
            assertNotNull(meta.releaseDate, "ReleaseDate: ${meta.releaseDate}")
        }

        val tcrFile = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.tcr")
        if (tcrFile.exists()) {
            val meta = BookExtractorFactory.getMetadata(tcrFile.absolutePath)
            assertTrue(meta.title.contains("86--EIGHTY-SIX"), "Title: ${meta.title}")
            assertFalse(meta.title.startsWith("01-"), "Title should strip 01- prefix: ${meta.title}")
            assertEquals("Yen On", meta.publisher, "Publisher: ${meta.publisher}")
            assertEquals(1, meta.seriesIndex, "SeriesIndex: ${meta.seriesIndex}")
            assertNotNull(meta.releaseDate, "ReleaseDate: ${meta.releaseDate}")
        }
    }

    @Test
    fun testLrfAndLitMetadata() = runBlocking {
        val lrfFile = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.lrf")
        if (lrfFile.exists()) {
            val meta = BookExtractorFactory.getMetadata(lrfFile.absolutePath)
            assertTrue(meta.title.contains("86--EIGHTY-SIX"), "Title: ${meta.title}")
            assertTrue(meta.author.contains("Asato") && meta.author.contains("Shirabii"), "Author: ${meta.author}")
            assertEquals("Yen On", meta.publisher, "Publisher: ${meta.publisher}")
            assertEquals(1, meta.seriesIndex, "SeriesIndex: ${meta.seriesIndex}")
            assertTrue(meta.annotation.isNotBlank(), "Annotation: ${meta.annotation}")
        }

        val litFile = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.lit")
        if (litFile.exists()) {
            val meta = BookExtractorFactory.getMetadata(litFile.absolutePath)
            assertTrue(meta.title.contains("86--EIGHTY-SIX"), "Title: ${meta.title}")
            assertTrue(meta.author.contains("Asato") && meta.author.contains("Shirabii"), "Author: ${meta.author}")
            assertEquals("Yen On", meta.publisher, "Publisher: ${meta.publisher}")
            assertEquals("9781975303129", meta.isbn, "ISBN: ${meta.isbn}")
            assertTrue(meta.series.contains("86-EIGHTY-SIX"), "Series: ${meta.series}")
            assertEquals(1, meta.seriesIndex, "SeriesIndex: ${meta.seriesIndex}")
            assertTrue(meta.annotation.isNotBlank(), "Annotation: ${meta.annotation}")
        }
    }
}

