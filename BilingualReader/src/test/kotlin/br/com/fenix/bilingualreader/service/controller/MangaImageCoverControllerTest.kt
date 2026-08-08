package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.graphics.Bitmap
import br.com.fenix.bilingualreader.model.entity.Manga
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
class MangaImageCoverControllerTest {

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testGetMangaCover() {
        val context = mockk<Context>(relaxed = true)
        val manga = mockk<Manga>(relaxed = true)
        val mockBitmap = mockk<Bitmap>(relaxed = true)

        val spy = spyk(MangaImageCoverController.instance)
        every { spy.getMangaCover(any(), any(), any()) } returns mockBitmap

        val result = spy.getMangaCover(context, manga, true)
        assertNotNull(result)
        assertEquals(mockBitmap, result)
    }
}
