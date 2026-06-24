package br.com.fenix.bilingualreader.model.entity

import android.graphics.Bitmap
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PagesTest {

    @Test
    fun `test pages construction and properties`() {
        val bitmap = mockk<Bitmap>()
        val pages = Pages(
            name = "Page 1",
            number = 1,
            page = 5,
            isSelected = true,
            image = bitmap
        )

        assertEquals("Page 1", pages.name)
        assertEquals(1, pages.number)
        assertEquals(5, pages.page)
        assertTrue(pages.isSelected)
        assertEquals(bitmap, pages.image)
    }

    @Test
    fun `test pages default values`() {
        val pages = Pages("Page 1", 1, 1)
        
        assertFalse(pages.isSelected)
        assertEquals(null, pages.image)
    }
}
