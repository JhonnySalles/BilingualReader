package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.enums.Type
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookAnnotationTest {

    private fun createDefaultAnnotation(): BookAnnotation {
        return BookAnnotation(
            id_book = 1L,
            page = 5,
            pages = 100,
            fontSize = 12f,
            type = MarkType.Annotation,
            chapterNumber = 1.0f,
            chapter = "Chapter 1",
            text = "Highlighted text",
            range = intArrayOf(0, 10),
            annotation = "My note",
            favorite = false,
            color = Color.Yellow
        )
    }

    @Test
    fun `test book annotation construction and ignore properties`() {
        val annotation = createDefaultAnnotation()

        assertEquals(1L, annotation.id_parent)
        assertEquals(5, annotation.page)
        assertEquals(Type.BOOK, annotation.type)
        assertFalse(annotation.isRoot)
        assertFalse(annotation.isTitle)
        assertEquals(0, annotation.count)
    }

    @Test
    fun `test title constructor sets ignore properties`() {
        val titleAnno = BookAnnotation(
            id_book = 1L, chapterNumber = 1.0f, chapter = "Ch", text = "", annotation = "", 
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
            text = "Updated text"
            favorite = true
            color = Color.Blue
            count = 5
        }

        original.update(other)

        assertEquals(99L, original.id)
        assertEquals("Updated text", original.text)
        assertTrue(original.favorite)
        assertEquals(Color.Blue, original.color)
        assertEquals(5, original.count)
    }

    @Test
    fun `test copy constructor`() {
        val original = createDefaultAnnotation()
        original.count = 10
        original.isTitle = true
        
        val copy = BookAnnotation(original)

        assertEquals(original.id_parent, copy.id_parent)
        assertEquals(10, copy.count)
        assertTrue(copy.isTitle)
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
