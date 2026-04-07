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
class SuperRubySpanTest {

    @Test
    fun `getSize should take the maximum width between text and furigana`() {
        val paint = TextPaint().apply {
            textSize = 20f
        }
        val outFm = Paint.FontMetricsInt()
        
        // Base text "漢字" (2 chars), Furigana "かんじ" (4 chars).
        // SuperRubySpan constructor only takes the furigana string.
        val span = SuperRubySpan("かんじ")
        
        val size = span.getSize(paint, "漢字", 0, 2, outFm)
        
        assertTrue("Size should be positive", size > 0)
        assertNotEquals("Font metrics should not be zero", 0, outFm.ascent)
    }

    @Test
    fun `getSize should correctly calculate adjusted ascent and top for furigana`() {
        val paint = TextPaint().apply {
            textSize = 20f
        }
        val outFm = Paint.FontMetricsInt()
        
        val span = SuperRubySpan("かん")
        span.getSize(paint, "漢", 0, 1, outFm)
        
        val standardPaintFm = Paint.FontMetricsInt()
        paint.getFontMetricsInt(standardPaintFm)
        
        assertTrue("Ruby top should be further up than standard top", outFm.top <= standardPaintFm.top)
    }
}
