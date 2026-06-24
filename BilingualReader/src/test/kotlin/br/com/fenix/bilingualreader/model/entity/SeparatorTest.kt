package br.com.fenix.bilingualreader.model.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SeparatorTest {

    @Test
    fun `test separator construction`() {
        val separator = Separator("Title", 5)
        assertEquals("Title", separator.title)
        assertEquals(5, separator.items)
    }

    @Test
    fun `test separator default values`() {
        val separator = Separator("Title")
        assertEquals(0, separator.items)
    }

    @Test
    fun `test separator equality based on title`() {
        val sep1 = Separator("Same", 1)
        val sep2 = Separator("Same", 2)
        val sep3 = Separator("Different", 1)

        assertEquals(sep1, sep2)
        assertNotEquals(sep1, sep3)
        assertEquals(sep1.hashCode(), sep2.hashCode())
    }
}
