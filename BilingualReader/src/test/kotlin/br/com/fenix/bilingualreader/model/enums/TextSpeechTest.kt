package br.com.fenix.bilingualreader.model.enums

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TextSpeechTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `test getTextSpeech mapping by description`() {
        // We test the lookup logic. Since we are in Robolectric, 
        // getString will return the actual values from strings.xml
        val nanamiDescription = TextSpeech.NANAMI.getDescription(context)
        
        val found = TextSpeech.getTextSpeech(context, nanamiDescription, true)
        assertEquals(TextSpeech.NANAMI, found)
    }

    @Test
    fun `test getDefault by language`() {
        assertEquals(TextSpeech.NANAMI, TextSpeech.getDefault(true))
        assertEquals(TextSpeech.FRANCISCA, TextSpeech.getDefault(false))
    }

    @Test
    fun `test getByDescriptions filters active voices`() {
        val activeVoices = TextSpeech.getByDescriptions(context)
        
        // Ensure active voices like ARIA and NANAMI are present
        assertTrue(activeVoices.values.contains(TextSpeech.ARIA))
        assertTrue(activeVoices.values.contains(TextSpeech.NANAMI))
        
        // DAVIS is marked isActive = false in the enum
        assertFalse(activeVoices.values.contains(TextSpeech.DAVIS))
    }

    private fun assertFalse(condition: Boolean) {
        org.junit.Assert.assertFalse(condition)
    }
}
