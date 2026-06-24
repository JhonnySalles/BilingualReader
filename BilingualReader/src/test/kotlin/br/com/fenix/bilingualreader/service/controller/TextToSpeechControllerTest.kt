package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.content.SharedPreferences
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.enums.TextSpeech
import br.com.fenix.bilingualreader.service.listener.TTSListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import io.github.whitemagic2014.tts.TTSVoice
import io.github.whitemagic2014.tts.bean.Voice
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

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
        
        // Mock GeneralConsts.getCacheDir to return a valid File
        mockkObject(GeneralConsts.Companion)
        every { GeneralConsts.getCacheDir(any()) } returns File("C:\\temp") // Using a path that works on Windows
        
        // Mock TTS classes that are used in init
        mockkStatic(TTSVoice::class)
        val mockVoice = mockk<Voice>(relaxed = true)
        every { mockVoice.shortName } returns "test-voice"
        every { TTSVoice.provides() } returns listOf(mockVoice)

        // Mock Firebase
        mockkStatic(Firebase::class)
        mockkStatic("com.google.firebase.crashlytics.ktx.FirebaseCrashlyticsKt")
        val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { Firebase.crashlytics } returns crashlytics

        // Mock SharedPreferences
        val sharedPrefs = mockk<SharedPreferences>(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs
        every { sharedPrefs.getString(any(), any()) } returns TextSpeech.getDefault(false).toString()
        
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
