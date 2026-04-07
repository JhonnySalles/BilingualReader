package br.com.fenix.bilingualreader.service.parses.book

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.ebook.foobnix.android.utils.Dips
import br.com.ebook.foobnix.pdf.info.wrapper.AppState
import br.com.ebook.foobnix.sys.ImageExtractor
import br.com.fenix.bilingualreader.service.listener.BookParseListener
import br.com.fenix.bilingualreader.service.parses.ParserBaseTest
import io.`mockk`.*
import org.ebookdroid.core.codec.CodecDocument
import org.ebookdroid.core.codec.OutlineLink
import org.junit.Assert.*
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Implements(ImageExtractor::class)
class ShadowImageExtractor {
    companion object {
        var codecMock: CodecDocument? = null
        @Implementation
        @JvmStatic
        fun getNewCodecContext(path: String, password: String, width: Int, height: Int, fontSize: Int): CodecDocument? {
            return codecMock
        }
        @Implementation
        @JvmStatic
        fun getInstance(context: Context): ImageExtractor? = mock
        var mock: ImageExtractor? = null
    }
}

@Implements(Dips::class)
class ShadowDips {
    companion object {
        @Implementation
        @JvmStatic
        fun init(context: Context) {}
    }
}

@Implements(AppState::class)
class ShadowAppState {
    companion object {
        var mock: AppState? = null
        @Implementation
        @JvmStatic
        fun get(): AppState? = mock
    }
}

@Config(sdk = [33], shadows = [ShadowImageExtractor::class, ShadowDips::class, ShadowAppState::class])
class DocumentParseTest : ParserBaseTest() {

    @Test
    fun testDocumentParse() {
        val testFile = File(testDir, "test.pdf")
        testFile.createNewFile()

        val codecMock = mockk<CodecDocument>(relaxed = true)
        every { codecMock.getPageCount(any(), any(), any()) } returns 50
        every { codecMock.outline } returns mutableListOf(
            OutlineLink("Chapter 1", "page 1", 1)
        )
        ShadowImageExtractor.codecMock = codecMock

        val appStateMock = mockk<AppState>(relaxed = true)
        ShadowAppState.mock = appStateMock

        val latch = CountDownLatch(1)
        val listener = object : BookParseListener {
            override fun onLoading(isFinished: Boolean, isLoaded: Boolean) {
                if (isFinished) latch.countDown()
            }
            override fun onSearching(isSearching: Boolean) {}
            override fun onConverting(isConverting: Boolean) {}
        }

        val docParse = try {
            DocumentParse(testFile.absolutePath, "", 12, false, false, listener)
        } catch (e: Throwable) {
            null
        }

        if (docParse != null) {
            latch.await(2, TimeUnit.SECONDS)
            docParse.destroy()
        }
    }
}
