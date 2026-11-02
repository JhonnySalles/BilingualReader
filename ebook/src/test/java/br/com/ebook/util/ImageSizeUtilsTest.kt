package br.com.ebook.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.File
import java.util.zip.ZipFile

class ImageSizeUtilsTest {

    private val testDir = sequenceOf(File("../teste"), File("teste"), File("f:/Projetos/BilingualReader/teste"))
        .firstOrNull { it.exists() && it.isDirectory } ?: File("../teste")

    @Test
    fun testClassificationRules() {
        val screenW = 1080
        val screenH = 1920
        val minSide = 64

        // 1. Large illustrations -> FULL_PAGE
        assertEquals(
            ImageSizeUtils.ImageRole.FULL_PAGE,
            ImageSizeUtils.classify(Pair(1200, 1800), screenW, screenH, minSide)
        )
        assertEquals(
            ImageSizeUtils.ImageRole.FULL_PAGE,
            ImageSizeUtils.classify(Pair(1800, 1350), screenW, screenH, minSide)
        )

        // 2. Small ornaments -> INLINE
        assertEquals(
            ImageSizeUtils.ImageRole.INLINE,
            ImageSizeUtils.classify(Pair(23, 47), screenW, screenH, minSide)
        )

        // 3. Thin lines (large width, but height < minSide) -> INLINE
        assertEquals(
            ImageSizeUtils.ImageRole.INLINE,
            ImageSizeUtils.classify(Pair(500, 1), screenW, screenH, minSide)
        )
        assertEquals(
            ImageSizeUtils.ImageRole.INLINE,
            ImageSizeUtils.classify(Pair(60, 1), screenW, screenH, minSide)
        )
        assertEquals(
            ImageSizeUtils.ImageRole.INLINE,
            ImageSizeUtils.classify(Pair(26, 2), screenW, screenH, minSide)
        )

        // 4. Null dimensions / bytes -> INLINE
        assertEquals(
            ImageSizeUtils.ImageRole.INLINE,
            ImageSizeUtils.classify(null as Pair<Int, Int>?, screenW, screenH, minSide)
        )
        assertEquals(
            ImageSizeUtils.ImageRole.INLINE,
            ImageSizeUtils.classify(null as ByteArray?, screenW, screenH, minSide)
        )
    }

    @Test
    fun testRealEpubImagesDimensions() {
        val epubFile = File(testDir, "01-86--EIGHTY-SIX, Vol. 01.epub")
        if (!epubFile.exists()) return

        ZipFile(epubFile).use { zip ->
            // Cover
            val coverEntry = zip.getEntry("OEBPS/Images/9781975303136.jpg")
            assertNotNull(coverEntry)
            val coverBytes = zip.getInputStream(coverEntry).readBytes()
            val coverDims = ImageSizeUtils.readDimensions(coverBytes)
            assertNotNull(coverDims)
            assertEquals(1466, coverDims!!.first)
            assertEquals(2200, coverDims.second)
            assertEquals(ImageSizeUtils.ImageRole.FULL_PAGE, ImageSizeUtils.classify(coverDims))

            // Thin line
            val lineEntry = zip.getEntry("OEBPS/Images/Art_line1.jpg")
            assertNotNull(lineEntry)
            val lineBytes = zip.getInputStream(lineEntry).readBytes()
            val lineDims = ImageSizeUtils.readDimensions(lineBytes)
            assertNotNull(lineDims)
            assertEquals(500, lineDims!!.first)
            assertEquals(1, lineDims.second)
            assertEquals(ImageSizeUtils.ImageRole.INLINE, ImageSizeUtils.classify(lineDims))

            // Small ornament
            val sbornEntry = zip.getEntry("OEBPS/Images/Art_sborn.jpg")
            assertNotNull(sbornEntry)
            val sbornBytes = zip.getInputStream(sbornEntry).readBytes()
            val sbornDims = ImageSizeUtils.readDimensions(sbornBytes)
            assertNotNull(sbornDims)
            assertEquals(23, sbornDims!!.first)
            assertEquals(47, sbornDims.second)
            assertEquals(ImageSizeUtils.ImageRole.INLINE, ImageSizeUtils.classify(sbornDims))

            // Insert illustration
            val insertEntry = zip.getEntry("OEBPS/Images/Art_insert4.jpg")
            assertNotNull(insertEntry)
            val insertBytes = zip.getInputStream(insertEntry).readBytes()
            val insertDims = ImageSizeUtils.readDimensions(insertBytes)
            assertNotNull(insertDims)
            assertEquals(1800, insertDims!!.first)
            assertEquals(1350, insertDims.second)
            assertEquals(ImageSizeUtils.ImageRole.FULL_PAGE, ImageSizeUtils.classify(insertDims))
        }
    }
}
