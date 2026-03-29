package br.com.fenix.bilingualreader.service.parses.book

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import br.com.ebook.foobnix.sys.ImageExtractor
import br.com.fenix.bilingualreader.service.parses.ParserBaseTest
import io.mockk.*
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class ImageParseTest : ParserBaseTest() {

    private lateinit var context: Context

    @Before
    override fun setUp() {
        super.setUp()
        context = ApplicationProvider.getApplicationContext()
        
        // Mocking System.loadLibrary to avoid UnsatisfiedLinkError
        mockkStatic(System::class)
        every { System.loadLibrary(any()) } returns Unit
        
        // Mocking external static init methods if needed
        mockkObject(ImageParse.Companion)
        every { ImageParse.init(any()) } returns Unit
    }

    @Test
    fun testGetCoverPage() {
        val mockBitmap = mockk<Bitmap>()
        val mockExtractor = mockk<ImageExtractor>()
        
        mockkStatic(ImageExtractor::class)
        every { ImageExtractor.getInstance(any()) } returns mockExtractor
        every { mockExtractor.proccessCoverPage(any()) } returns mockBitmap

        val imageParse = ImageParse(context)
        val result = imageParse.getCoverPage("dummy_path", true)

        assertNotNull(result)
        verify { mockExtractor.proccessCoverPage(any()) }
    }
}
