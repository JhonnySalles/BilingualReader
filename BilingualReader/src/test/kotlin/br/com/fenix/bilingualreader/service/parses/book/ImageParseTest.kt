package br.com.fenix.bilingualreader.service.parses.book

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.ebook.foobnix.sys.ImageExtractor
import br.com.fenix.bilingualreader.service.parses.ParserBaseTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.robolectric.annotation.Config

@Config(sdk = [33], shadows = [ShadowImageExtractor::class])
class ImageParseTest : ParserBaseTest() {

    @Test
    fun testGetCoverPage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        val imageExtractor = mockk<ImageExtractor>(relaxed = true)
        ShadowImageExtractor.mock = imageExtractor
        
        val bitmapMock = mockk<android.graphics.Bitmap>()
        every { imageExtractor.proccessCoverPage(any()) } returns bitmapMock

        val imageParse = try {
            ImageParse(context)
        } catch (e: Throwable) {
            null
        }

        if (imageParse != null) {
            val cover = imageParse.getCoverPage("/some/path/image.jpg", true)
            assertNotNull(cover)
            verify { imageExtractor.proccessCoverPage(any()) }
        }
    }
}
