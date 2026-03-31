package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
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
class ImageControllerTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Dispatchers::class)
        every { Dispatchers.IO } returns testDispatcher
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun testSetImageAsync() = runTest {
        val context = mockk<Context>(relaxed = true)
        val imageView = mockk<ImageView>(relaxed = true)
        val link = "http://example.com/test.png"
        val mockBitmap = mockk<Bitmap>(relaxed = true)

        val spy = spyk(ImageController.instance, recordPrivateCalls = true)
        every { spy["getImage"](any<Context>(), any<String>()) } returns mockBitmap

        spy.setImageAsync(context, link, imageView)
        
        verify { imageView.setImageBitmap(mockBitmap) }
        verify { imageView.visibility = View.VISIBLE }
    }
}
