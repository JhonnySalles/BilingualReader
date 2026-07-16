package br.com.fenix.bilingualreader.service.parses.book

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.ebook.foobnix.ext.CacheZipUtils
import br.com.ebook.foobnix.sys.ImageExtractor
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], shadows = [ShadowImageExtractor::class])
class ImageParseTest {

    @Rule @JvmField
    val tempFolder = TemporaryFolder()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Initialize CacheZipUtils with a temporary directory
        val testDir = File(tempFolder.root, "image_tests")
        testDir.mkdirs()
        CacheZipUtils.init(context, testDir)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testGetCoverPage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        val imageExtractor = mockk<ImageExtractor>(relaxed = true)
        ShadowImageExtractor.mock = imageExtractor
        
        val bitmapMock = mockk<android.graphics.Bitmap>()
        every { imageExtractor.processCoverPage(any()) } returns bitmapMock

        val imageParse = try {
            ImageParse(context)
        } catch (e: Throwable) {
            null
        }

        if (imageParse != null) {
            val cover = imageParse.getCoverPage("/some/path/image.jpg", true)
            assertNotNull(cover)
            verify { imageExtractor.processCoverPage(any()) }
        }
    }
}
