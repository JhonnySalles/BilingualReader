package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.graphics.Bitmap
import br.com.fenix.bilingualreader.model.entity.Book
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BookImageCoverControllerTest {

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testGetBookCover() {
        val context = mockk<Context>(relaxed = true)
        val book = mockk<Book>(relaxed = true)
        val mockBitmap = mockk<Bitmap>(relaxed = true)

        val spy = spyk(BookImageCoverController.instance)
        every { spy.getBookCover(any(), any(), any()) } returns mockBitmap

        val result = spy.getBookCover(context, book, true)
        assertNotNull(result)
        assertEquals(mockBitmap, result)
    }
}
