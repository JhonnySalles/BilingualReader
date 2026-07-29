package br.com.fenix.bilingualreader.service.ocr

import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Implements(TessBaseAPI::class)
class ShadowTessBaseAPI {
    @Implementation
    fun __constructor__() {}

    @Implementation
    fun init(datapath: String, language: String): Boolean = true

    @Implementation
    fun setImage(bitmap: Bitmap) {}

    @Implementation
    fun getUtF8Text(): String = "Extracted Text"

    @Implementation
    fun getUTF8Text(): String = "Extracted Text"

    @Implementation
    fun recycle() {}
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [ShadowTessBaseAPI::class])
class TesseractApiTest {

    @Test
    fun `TesseractApiImpl should delegate init, setImage, utF8Text, and recycle`() {
        val api: TesseractApi = TesseractApiImpl()

        val initResult = api.init("/path/to/datapath", "jpn")
        assertTrue(initResult)

        val text = api.utF8Text
        assertEquals("Extracted Text", text)

        api.recycle()
    }
}
