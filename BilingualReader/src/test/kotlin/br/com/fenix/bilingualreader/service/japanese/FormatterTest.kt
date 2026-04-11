package br.com.fenix.bilingualreader.service.japanese

import android.content.Context
import android.graphics.Color
import br.com.fenix.bilingualreader.service.repository.KanjaxRepository
import br.com.fenix.bilingualreader.service.repository.KanjiRepository
import br.com.fenix.bilingualreader.service.repository.VocabularyRepository
import br.com.fenix.bilingualreader.service.tokenizers.SudachiTokenizer
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FormatterTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        
        // Mock repositories
        mockkConstructor(KanjaxRepository::class)
        mockkConstructor(VocabularyRepository::class)
        mockkConstructor(KanjiRepository::class)
        mockkConstructor(SudachiTokenizer::class)

        // Mock getColor to avoid real resource issues
        every { context.getColor(any()) } returns Color.BLACK
        
        // Mock Firebase
        mockkStatic(Firebase::class)
        mockkStatic("com.google.firebase.crashlytics.ktx.FirebaseCrashlyticsKt")
        val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { Firebase.crashlytics } returns crashlytics
        
        // Initialize Formatter (companion object JAPANESE)
        Formatter.initializeAsync(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun generateHtmlText_withPlainJapanese_returnsSameTextIfNoTokenizer() {
        // Sudachi needs to be null or mocked
        val text = "テスト"
        val result = Formatter.generateHtmlText(text, false)
        assertEquals(text, result)
    }

    @Test
    fun generateKanjiColor_appliesColorToKanji() {
        val kanji = "日"
        var result: android.text.SpannableString? = null
        
        // We need to bypass or mock the internal JLPT map if we want specific colors
        // Since it's private, we'll just check if a ClickableSpan was added
        
        Formatter.generateKanjiColor(kanji, { result = it }, { _, _ -> })
        
        assertNotNull(result)
        val spans = result!!.getSpans(0, result!!.length, android.text.style.ClickableSpan::class.java)
        assertEquals(1, spans.size)
    }

    @Test
    fun getCss_returnsColorsString() {
        val css = Formatter.getCss()
        assertTrue(css.contains(".n1 {color: #ff4d4d }"))
        assertTrue(css.contains(".n5 {color: #b366ff }"))
    }
}
