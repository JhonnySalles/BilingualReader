package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import br.com.fenix.bilingualreader.model.entity.Manga
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MangaImageCoverControllerTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(MangaImageCoverController.Companion)
        try {
            every { MangaImageCoverController.thread } returns testDispatcher
        } catch (e: Exception) {}
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun testSetImageCoverAsync() = runTest {
        val context = mockk<Context>(relaxed = true)
        val imageView = mockk<ImageView>(relaxed = true)
        val manga = mockk<Manga>(relaxed = true)
        val mockBitmap = mockk<Bitmap>(relaxed = true)

        val instance = MangaImageCoverController.instance
        val spy = spyk(instance)
        every { spy.getMangaCover(any(), any(), any()) } returns mockBitmap

        spy.setImageCoverAsync(context, manga, imageView, null, true)
        
        // Advance time if necessary (though Unconfined should be immediate)
        advanceUntilIdle()
        
        verify { imageView.setImageBitmap(mockBitmap) }
    }
}
