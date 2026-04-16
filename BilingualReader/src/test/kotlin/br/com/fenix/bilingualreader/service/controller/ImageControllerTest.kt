package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class ImageControllerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        ImageController.instance.ioDispatcher = testDispatcher
        ImageController.instance.mainDispatcher = testDispatcher
        ImageController.instance.imageScope = testScope
    }

    @After
    fun tearDown() {
        ImageController.instance.ioDispatcher = Dispatchers.IO
        ImageController.instance.mainDispatcher = Dispatchers.Main
        ImageController.instance.imageScope = null
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
        
        // Advance time to ensure the coroutine in the separate scope (using mocked Dispatchers) completes
        testScheduler.advanceUntilIdle()
        
        verify { imageView.setImageBitmap(mockBitmap) }
        verify { imageView.visibility = View.VISIBLE }
    }
}
