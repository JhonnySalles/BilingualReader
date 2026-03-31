package br.com.fenix.bilingualreader.service.ocr

import android.content.Context
import android.graphics.Bitmap
import br.com.fenix.bilingualreader.model.enums.Languages
import com.googlecode.tesseract.android.TessBaseAPI
import io.mockk.*
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TesseractTest {

    private lateinit var context: Context
    private val tessApi: TessBaseAPI = mockk(relaxed = true)

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        
        // Mock Tesseract static to avoid real asset copying in init
        mockkObject(Tesseract.Companion)
        every { Tesseract.copyTessData(any()) } just Runs
        
        // Mock ImageProcess static
        mockkObject(ImageProcess.Companion)
        every { ImageProcess.processGrayscale(any()) } returns mockk(relaxed = true)

        // Mock behaviors on the mock instance
        every { tessApi.init(any(), any()) } returns true
        every { tessApi.utF8Text } returns "Detected Text"
        every { tessApi.recycle() } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun process_initializesWithCorrectLanguage() {
        val tesseract = Tesseract(context) { tessApi }
        val bitmap = mockk<Bitmap>(relaxed = true)
        
        // Test Japanese
        tesseract.process(Languages.JAPANESE, bitmap)
        verify { tessApi.init(any(), "jpn") }
        
        // Test English
        tesseract.process(Languages.ENGLISH, bitmap)
        verify { tessApi.init(any(), "eng") }
    }

    @Test
    fun process_returnsRecognizedText() {
        val tesseract = Tesseract(context) { tessApi }
        val bitmap = mockk<Bitmap>(relaxed = true)
        
        val result = tesseract.process(Languages.ENGLISH, bitmap)
        
        assertNotNull("Result should not be null", result)
        assertEquals("Detected Text", result)
        verify { tessApi.setImage(any<Bitmap>()) }
        verify { tessApi.recycle() }
    }

    @Test
    fun process_returnsNullIfInitFails() {
        every { tessApi.init(any(), any()) } returns false
        
        val tesseract = Tesseract(context) { tessApi }
        val bitmap = mockk<Bitmap>(relaxed = true)
        
        val result = tesseract.process(Languages.ENGLISH, bitmap)
        
        assertNull("Result should be null when init fails", result)
    }
}
