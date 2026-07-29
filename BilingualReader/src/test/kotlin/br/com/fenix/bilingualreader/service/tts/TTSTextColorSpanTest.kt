package br.com.fenix.bilingualreader.service.tts

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TTSTextColorSpanTest {

    @Test
    fun `span should store correct color value`() {
        val color = Color.RED
        val span = TTSTextColorSpan(color)

        assertEquals(color, span.foregroundColor)
    }
}
