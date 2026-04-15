package br.com.fenix.bilingualreader.model.entity

import android.graphics.Bitmap
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.enums.Type
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MangaAnnotationTest {

    private fun createDefaultAnnotation(): MangaAnnotation {
        val now = LocalDateTime.of(2023, 1, 1, 10, 0)
        return MangaAnnotation(
            null, 1L, 5, 100, MarkType.PageMark, "Chapter 1", "/folder/1", "My note", now, now
        )
    }

    @Test
    fun `test manga annotation construction and ignore properties`() {
        val annotation = createDefaultAnnotation()

        assertEquals(1L, annotation.id_parent)
        assertEquals(5, annotation.page)
        assertEquals(Type.MANGA, annotation.type)
        assertFalse(annotation.isRoot)
        assertFalse(annotation.isTitle)
        assertEquals(0, annotation.count)
    }

    @Test
    fun `test title constructor sets ignore properties`() {
        val titleAnno = MangaAnnotation(
            id_manga = 1L, chapter = "Ch", folder = "", annotation = "", 
            isRoot = true, isTitle = true
        )

        assertTrue(titleAnno.isRoot)
        assertTrue(titleAnno.isTitle)
        assertEquals(0, titleAnno.count)
    }

    @Test
    fun `test update copies all fields`() {
        val original = createDefaultAnnotation()
        val other = createDefaultAnnotation().apply {
            id = 99L
            annotation = "Updated note"
            count = 5
        }

        original.update(other)

        assertEquals(99L, original.id)
        assertEquals("Updated note", original.annotation)
        assertEquals(5, original.count)
    }

    @Test
    fun `test copy constructor`() {
        val bitmap = mockk<Bitmap>()
        val original = createDefaultAnnotation()
        original.count = 10
        original.image = bitmap
        
        val copy = MangaAnnotation(original)

        assertEquals(original.id_parent, copy.id_parent)
        assertEquals(10, copy.count)
        assertEquals(bitmap, copy.image)
    }

    @Test
    fun `test equality uses subset of fields`() {
        val a1 = createDefaultAnnotation()
        val a2 = createDefaultAnnotation()
        val a3 = createDefaultAnnotation().apply { page = 10 }

        assertEquals(a1, a2)
        assertNotEquals(a1, a3)
        assertEquals(a1.hashCode(), a2.hashCode())
    }
}
