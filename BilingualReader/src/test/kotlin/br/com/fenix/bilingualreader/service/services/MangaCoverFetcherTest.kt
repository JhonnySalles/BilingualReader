package br.com.fenix.bilingualreader.service.services

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
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
class MangaCoverFetcherTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(MangaImageCoverController.Companion)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `fetch should return SourceResult when manga cover file exists`() = runTest {
        val context = mockk<Context>(relaxed = true)
        val options = mockk<Options>(relaxed = true)
        every { options.context } returns context

        val manga = mockk<Manga>(relaxed = true)
        val mockFile = File.createTempFile("test_manga_cover", ".jpg")
        mockFile.deleteOnExit()

        val controllerMock = mockk<MangaImageCoverController>()
        every { MangaImageCoverController.instance } returns controllerMock
        every { MangaImageCoverController.thread } returns testDispatcher
        every { controllerMock.getMangaCoverFile(context, manga, true) } returns mockFile

        val fetcher = MangaCoverFetcher(manga, options)
        val result = fetcher.fetch()

        assertNotNull(result)
        assertEquals(SourceResult::class.java, result!!::class.java)
    }

    @Test
    fun `fetch should return null when manga cover file is null`() = runTest {
        val context = mockk<Context>(relaxed = true)
        val options = mockk<Options>(relaxed = true)
        every { options.context } returns context

        val manga = mockk<Manga>(relaxed = true)

        val controllerMock = mockk<MangaImageCoverController>()
        every { MangaImageCoverController.instance } returns controllerMock
        every { MangaImageCoverController.thread } returns testDispatcher
        every { controllerMock.getMangaCoverFile(context, manga, true) } returns null

        val fetcher = MangaCoverFetcher(manga, options)
        val result = fetcher.fetch()

        assertNull(result)
    }

    @Test
    fun `factory should create MangaCoverFetcher instance`() {
        val manga = mockk<Manga>(relaxed = true)
        val options = mockk<Options>(relaxed = true)
        val imageLoader = mockk<ImageLoader>(relaxed = true)

        val factory = MangaCoverFetcher.Factory()
        val fetcher = factory.create(manga, options, imageLoader)

        assertNotNull(fetcher)
        assertEquals(MangaCoverFetcher::class.java, fetcher::class.java)
    }
}
