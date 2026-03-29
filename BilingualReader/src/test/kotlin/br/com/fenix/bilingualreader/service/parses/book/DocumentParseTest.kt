package br.com.fenix.bilingualreader.service.parses.book

import br.com.fenix.bilingualreader.service.listener.BookParseListener
import br.com.fenix.bilingualreader.service.parses.ParserBaseTest
import br.com.ebook.foobnix.sys.ImageExtractor
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.ebookdroid.core.codec.CodecDocument

class DocumentParseTest : ParserBaseTest() {

    @Before
    override fun setUp() {
        super.setUp()
        
        // Mocking System.loadLibrary to avoid UnsatisfiedLinkError
        mockkStatic(System::class)
        every { System.loadLibrary(any()) } returns Unit
        
        // Mocking DocumentParse.init to avoid Android specific init
        mockkObject(DocumentParse.Companion)
        every { DocumentParse.init(any()) } returns Unit
    }

    @Test
    fun testDocumentParseOpen() {
        val mockCodecFile = createSampleFile("test.pdf")
        val mockCodecDocument = mockk<CodecDocument>(relaxed = true)
        
        mockkStatic(ImageExtractor::class)
        every { ImageExtractor.getNewCodecContext(any(), any(), any(), any(), any()) } returns mockCodecDocument
        
        val latch = CountDownLatch(1)
        val listener = object : BookParseListener {
            override fun onLoading(isFinished: Boolean, isLoaded: Boolean) {
                if (isFinished) {
                    latch.countDown()
                }
            }
            override fun onSearching(isSearching: Boolean) {}
            override fun onConverting(isConverting: Boolean) {}
        }

        val docParse = DocumentParse(
            path = mockCodecFile.absolutePath, 
            fontSize = 12, 
            isLandscape = false, 
            isVertical = false, 
            listener = listener
        )

        // Wait for openBook thread to finish
        latch.await(5, TimeUnit.SECONDS)

        assertTrue(docParse.isLoaded())
        assertFalse(docParse.isLoading())
        
        docParse.destroy()
    }

    @Test
    fun testDocumentParseFail() {
        val mockCodecFile = createSampleFile("invalid.pdf")
        
        mockkStatic(ImageExtractor::class)
        every { ImageExtractor.getNewCodecContext(any(), any(), any(), any(), any()) } returns null
        
        val latch = CountDownLatch(1)
        val listener = object : BookParseListener {
            override fun onLoading(isFinished: Boolean, isLoaded: Boolean) {
                if (isFinished) {
                    latch.countDown()
                }
            }
            override fun onSearching(isSearching: Boolean) {}
            override fun onConverting(isConverting: Boolean) {}
        }

        val docParse = DocumentParse(
            path = mockCodecFile.absolutePath, 
            fontSize = 12, 
            isLandscape = false, 
            isVertical = false, 
            listener = listener
        )

        latch.await(5, TimeUnit.SECONDS)

        assertFalse(docParse.isLoaded())
        
        docParse.destroy()
    }
}
