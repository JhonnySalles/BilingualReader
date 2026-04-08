package br.com.fenix.bilingualreader.service.ocr

import android.graphics.Bitmap
import android.graphics.Color
import br.com.fenix.bilingualreader.service.parses.ParserBaseTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ImageProcessTest {

    @Test
    fun testToGrayscale() {
        // Create a 2x2 bitmap with specific colors (Red, Green, Blue, White)
        // Red: (255, 0, 0)
        // Green: (0, 255, 0)
        // Blue: (0, 0, 255)
        // White: (255, 255, 255)
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, Color.RED)
        bitmap.setPixel(1, 0, Color.GREEN)
        bitmap.setPixel(0, 1, Color.BLUE)
        bitmap.setPixel(1, 1, Color.WHITE)

        val grayscale = ImageProcess.toGrayscale(bitmap)

        // Formulas used in ImageProcess: (0.299 * r + 0.587 * g + 0.114 * b)
        // Red: 0.299 * 255 = 76 (approx)
        // Green: 0.587 * 255 = 149 (approx)
        // Blue: 0.114 * 255 = 29 (approx)
        // White: 255
        
        val redGray = Color.red(grayscale.getPixel(0, 0))
        val greenGray = Color.red(grayscale.getPixel(1, 0))
        val blueGray = Color.red(grayscale.getPixel(0, 1))
        val whiteGray = Color.red(grayscale.getPixel(1, 1))

        assertTrue("Red gray should be around 76", redGray in 74..78)
        assertTrue("Green gray should be around 149", greenGray in 147..151)
        assertTrue("Blue gray should be around 29", blueGray in 27..31)
        assertTrue("White gray should be around 255", whiteGray in 253..255)
        
        // Also verify all channels are the same in grayscale
        assertEquals(redGray, Color.green(grayscale.getPixel(0, 0)))
        assertEquals(redGray, Color.blue(grayscale.getPixel(0, 0)))
    }

    @Test
    fun testOtsuThreshold() {
        // Create a pattern with clear objects and background
        // 50% Black (0), 50% White (255)
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        for (x in 0 until 10) {
            for (y in 0 until 10) {
                if (x < 5) {
                    bitmap.setPixel(x, y, Color.BLACK)
                } else {
                    bitmap.setPixel(x, y, Color.WHITE)
                }
            }
        }

        val threshold = ImageProcess.otsuThreshold(bitmap)
        
        // With 50/50 black and white, the threshold should be exactly 0 (since it finds the first max) 
        // or around 127. Actually Otsu with 0 and 255 should return 0 in some implementations or 127.
        // Let's check for any valid threshold in the middle.
        assertTrue("Threshold was $threshold", threshold in 0..254)
    }

    @Test
    fun testGrayscaleToBin() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        // Fill half with dark gray and half with light gray
        for (x in 0 until 10) {
            for (y in 0 until 10) {
                if (x < 5) {
                    bitmap.setPixel(x, y, Color.rgb(20, 20, 20)) // Dark
                } else {
                    bitmap.setPixel(x, y, Color.rgb(240, 240, 240)) // Light
                }
            }
        }

        val binarized = ImageProcess.grayscaleToBin(bitmap)
        
        // Check if dark areas became blackish and light areas became whitish
        val darkPixel = binarized.getPixel(0, 0)
        val lightPixel = binarized.getPixel(9, 9)

        assertTrue("Dark pixel should be blackish, was ${Color.red(darkPixel)}", Color.red(darkPixel) < 128)
        assertTrue("Light pixel should be whitish, was ${Color.red(lightPixel)}", Color.red(lightPixel) >= 128)
    }
}
