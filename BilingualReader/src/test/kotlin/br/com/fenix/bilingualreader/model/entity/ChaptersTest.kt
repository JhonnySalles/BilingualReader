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
class ChaptersTest {

    @Test
    fun `test chapters construction and properties`() {
        val bitmap = mockk<Bitmap>()
        val parent = Chapters("Parent", 0, 0, 0.0f, true)
        val chapters = Chapters(
            title = "Title",
            number = 1,
            page = 10,
            chapter = 1.0f,
            isTitle = false,
            parent = parent,
            isSelected = true,
            image = bitmap
        )

        assertEquals("Title", chapters.title)
        assertEquals(1, chapters.number)
        assertEquals(10, chapters.page)
        assertEquals(1.0f, chapters.chapter)
        assertFalse(chapters.isTitle)
        assertEquals(parent, chapters.parent)
        assertTrue(chapters.isSelected)
        assertEquals(bitmap, chapters.image)
    }

    @Test
    fun `test chapters default values`() {
        val chapters = Chapters("Title", 1, 1, 1.0f, true)
        
        assertFalse(chapters.isSelected)
        assertEquals(null, chapters.image)
        assertEquals(null, chapters.parent)
    }
}
