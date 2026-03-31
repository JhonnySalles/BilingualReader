package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import br.com.fenix.bilingualreader.model.entity.Book
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
class BookImageCoverControllerTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(BookImageCoverController.Companion)
        try {
            every { BookImageCoverController.thread } returns testDispatcher
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
        val book = mockk<Book>(relaxed = true)
        val mockBitmap = mockk<Bitmap>(relaxed = true)

        val spy = spyk(BookImageCoverController.instance)
        // Method is public, so no string needed
        every { spy.getBookCover(any(), any(), any()) } returns mockBitmap

        spy.setImageCoverAsync(context, book, imageView, null, true)
        
        verify { imageView.setImageBitmap(mockBitmap) }
    }
}
