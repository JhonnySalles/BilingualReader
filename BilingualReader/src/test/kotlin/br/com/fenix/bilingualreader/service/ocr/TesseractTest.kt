package br.com.fenix.bilingualreader.service.ocr

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TesseractTest {

    private lateinit var context: Context
    private lateinit var tesseractApi: TesseractApi

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        tesseractApi = mockk(relaxed = true)
        
        // Use mockkObject for Kotlin Companion objects
        mockkObject(GeneralConsts.Companion)
        every { GeneralConsts.getCacheDir(any()) } returns context.cacheDir
    }

    @Test
    fun testProcessSyncJapanese() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        
        // Setup Tesseract behavior
        every { tesseractApi.init(any(), "jpn") } returns true
        every { tesseractApi.utF8Text } returns "日本語"

        val tesseract = Tesseract(context, tessApiFactory = { tesseractApi })
        
        // Force inCopy to false for the test
        Tesseract.inCopy = false
        
        val result = tesseract.process(Languages.JAPANESE, bitmap)

        assertEquals("日本語", result)
        
        verify { tesseractApi.init(any(), "jpn") }
        verify { tesseractApi.setImage(any()) }
    }

    @Test
    fun testProcessSyncEnglish() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        
        every { tesseractApi.init(any(), "eng") } returns true
        every { tesseractApi.utF8Text } returns "Hello"

        val tesseract = Tesseract(context, tessApiFactory = { tesseractApi })
        Tesseract.inCopy = false
        
        val result = tesseract.process(Languages.ENGLISH, bitmap)

        assertEquals("Hello", result)
        verify { tesseractApi.init(any(), "eng") }
    }

    @Test
    fun testProcessSyncFailedInit() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        
        every { tesseractApi.init(any(), any()) } returns false

        val tesseract = Tesseract(context, tessApiFactory = { tesseractApi })
        Tesseract.inCopy = false
        
        val result = tesseract.process(Languages.JAPANESE, bitmap)

        assertNull(result)
        verify { tesseractApi.init(any(), "jpn") }
    }
}
