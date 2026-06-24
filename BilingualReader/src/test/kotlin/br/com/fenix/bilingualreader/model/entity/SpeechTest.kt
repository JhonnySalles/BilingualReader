package br.com.fenix.bilingualreader.model.entity

import android.net.Uri
import androidx.media3.common.MediaItem
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SpeechTest {

    @Test
    fun `test speech construction and properties`() {
        val uri = mockk<Uri>()
        val mediaItem = mockk<MediaItem>()
        val speech = Speech(
            page = 1,
            sequence = 2,
            text = "Hello",
            html = "<b>Hello</b>",
            audio = uri,
            media = mediaItem,
            isRead = true
        )

        assertEquals(1, speech.page)
        assertEquals(2, speech.sequence)
        assertEquals("Hello", speech.text)
        assertEquals("<b>Hello</b>", speech.html)
        assertEquals(uri, speech.audio)
        assertEquals(mediaItem, speech.media)
        assertTrue(speech.isRead)
    }

    @Test
    fun `test speech default values`() {
        val speech = Speech(1, 1, "Text", "Html")
        
        assertEquals(null, speech.audio)
        assertEquals(null, speech.media)
        assertFalse(speech.isRead)
    }
}
