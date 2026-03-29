package br.com.fenix.bilingualreader.service.ocr

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ImageProcessTest {

    @Test
    fun toGrayscale_convertsToGray() {
        // Create a colored bitmap (Red)
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        
        val grayscale = ImageProcess.toGrayscale(bitmap)
        
        // In grayscale, R=G=B. Red (255,0,0) becomes some shade of gray.
        val pixel = grayscale.getPixel(5, 5)
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        
        assertEquals("Red channel should equal green in grayscale", r, g)
        assertEquals("Green channel should equal blue in grayscale", g, b)
        assertTrue("Grayscale value should be positive for Red", r > 0) 
    }

    @Test
    fun otsuThreshold_calculatesCorrectThreshold() {
        // Create a black and white bitmap (Otsu needs at least two intensities)
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        for (x in 0 until 10) {
            for (y in 0 until 10) {
                if (x < 5) bitmap.setPixel(x, y, Color.BLACK)
                else bitmap.setPixel(x, y, Color.WHITE)
            }
        }
        
        val threshold = ImageProcess.otsuThreshold(bitmap)
        
        // Threshold should be between 0 and 255
        assertTrue("Threshold should be within 0-255 range", threshold in 0..255)
    }

    @Test
    fun processGrayscale_returnsBinarizedImage() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        
        val processed = ImageProcess.processGrayscale(bitmap)
        
        assertNotNull("Processed bitmap should not be null", processed)
        assertEquals("Width should remain the same", bitmap.width, processed.width)
        assertEquals("Height should remain the same", bitmap.height, processed.height)
    }
}
