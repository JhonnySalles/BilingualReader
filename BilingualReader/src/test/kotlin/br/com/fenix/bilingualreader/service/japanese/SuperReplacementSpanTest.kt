package br.com.fenix.bilingualreader.service.japanese

import android.graphics.Paint
import android.text.TextPaint
import io.mockk.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SuperReplacementSpanTest {

    @Test
    fun `getSize should calculate total width and update font metrics`() {
        val testText = "日本語"
        val paint = TextPaint().apply {
            textSize = 20f
        }
        val outFm = Paint.FontMetricsInt()
        
        // SuperReplacementSpan is abstract, so let's use a real subclass or anonymous one
        val span = object : SuperReplacementSpan(SuperReplacementSpan.Alignment.CENTER) {}
        
        // In Robolectric, TextPaint and Paint methods work with real objects.
        // It should calculate size correctly without Mocking Paint.
        val size = span.getSize(paint, testText, 0, testText.length, outFm)
        
        assertTrue("Size should be positive", size > 0)
        // Verify outFm is updated (Robolectric shadows handle this)
        assertNotEquals("Font metrics should not be zero", 0, outFm.ascent)
    }

    @Test
    fun `Alignment enums should be defined correctly`() {
        assertEquals(0, SuperReplacementSpan.Alignment.BEGIN)
        assertEquals(1, SuperReplacementSpan.Alignment.END)
        assertEquals(2, SuperReplacementSpan.Alignment.CENTER)
        assertEquals(3, SuperReplacementSpan.Alignment.JUSTIFIED)
        assertEquals(4, SuperReplacementSpan.Alignment.JIS)
    }
}
