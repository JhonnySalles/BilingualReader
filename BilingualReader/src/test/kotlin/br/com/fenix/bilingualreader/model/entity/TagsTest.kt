package br.com.fenix.bilingualreader.model.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagsTest {

    @Test
    fun `test tags construction and properties`() {
        val tag = Tags(
            id = 1L,
            name = "TestTag",
            excluded = true,
            isSelected = true
        )

        assertEquals(1L, tag.id)
        assertEquals("TestTag", tag.name)
        assertTrue(tag.excluded)
        assertTrue(tag.isSelected)
    }

    @Test
    fun `test tags default values`() {
        val tag = Tags(null, "Tag")
        
        assertFalse(tag.excluded)
        assertFalse(tag.isSelected)
    }
}
