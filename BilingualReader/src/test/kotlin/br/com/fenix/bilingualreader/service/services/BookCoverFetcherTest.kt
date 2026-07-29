package br.com.fenix.bilingualreader.service.services

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import coil.ImageLoader
import coil.fetch.SourceResult
import coil.request.Options
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BookCoverFetcherTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(BookImageCoverController.Companion)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `fetch should return SourceResult when cover file exists`() = runTest {
        val context = mockk<Context>(relaxed = true)
        val options = mockk<Options>(relaxed = true)
        every { options.context } returns context

        val book = mockk<Book>(relaxed = true)
        val mockFile = File.createTempFile("test_cover", ".jpg")
        mockFile.deleteOnExit()

        val controllerMock = mockk<BookImageCoverController>()
        every { BookImageCoverController.instance } returns controllerMock
        every { BookImageCoverController.thread } returns testDispatcher
        every { controllerMock.getBookCoverFile(context, book, true) } returns mockFile

        val fetcher = BookCoverFetcher(book, options)
        val result = fetcher.fetch()

        assertNotNull(result)
        assertEquals(SourceResult::class.java, result!!::class.java)
    }

    @Test
    fun `fetch should return null when cover file is null`() = runTest {
        val context = mockk<Context>(relaxed = true)
        val options = mockk<Options>(relaxed = true)
        every { options.context } returns context

        val book = mockk<Book>(relaxed = true)

        val controllerMock = mockk<BookImageCoverController>()
        every { BookImageCoverController.instance } returns controllerMock
        every { BookImageCoverController.thread } returns testDispatcher
        every { controllerMock.getBookCoverFile(context, book, true) } returns null

        val fetcher = BookCoverFetcher(book, options)
        val result = fetcher.fetch()

        assertNull(result)
    }

    @Test
    fun `factory should create BookCoverFetcher instance`() {
        val book = mockk<Book>(relaxed = true)
        val options = mockk<Options>(relaxed = true)
        val imageLoader = mockk<ImageLoader>(relaxed = true)

        val factory = BookCoverFetcher.Factory()
        val fetcher = factory.create(book, options, imageLoader)

        assertNotNull(fetcher)
        assertEquals(BookCoverFetcher::class.java, fetcher::class.java)
    }
}
