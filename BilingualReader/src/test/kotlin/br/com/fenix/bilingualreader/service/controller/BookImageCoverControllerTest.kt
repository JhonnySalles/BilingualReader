package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import br.com.fenix.bilingualreader.model.entity.Book
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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

        val instance = BookImageCoverController.instance
        val spy = spyk(instance)
        // Method is public, so no string needed
        every { spy.getBookCover(any(), any(), any()) } returns mockBitmap

        spy.setImageCoverAsync(context, book, imageView, null, true)
        
        advanceUntilIdle()
        
        verify { imageView.setImageBitmap(mockBitmap) }
    }
}
