package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.service.listener.TTSListener
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TextToSpeechControllerTest {

    private lateinit var context: Context
    private lateinit var book: Book
    private lateinit var controller: TextToSpeechController
    
    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        book = mockk(relaxed = true)
        every { book.title } returns "Test Book"
        every { book.fileName } returns "test.epub"
        
        // Mock TTS classes that are used in init
        mockkStatic("io.github.whitemagic2014.tts.TTSVoice")
        every { io.github.whitemagic2014.tts.TTSVoice.provides() } returns emptyList()
        
        controller = TextToSpeechController(context, book, null, null, 12)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testAddRemoveListener() {
        val listener = mockk<TTSListener>()
        assertTrue(controller.addListener(listener))
        assertTrue(controller.removeListener(listener))
    }
}
