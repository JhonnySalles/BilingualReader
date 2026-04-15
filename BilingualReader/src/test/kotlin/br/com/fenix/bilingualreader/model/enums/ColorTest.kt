package br.com.fenix.bilingualreader.model.enums

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ColorTest {

    @Test
    fun `test color properties`() {
        val blue = Color.Blue
        assertEquals(0, blue.getIndex())
        assertEquals("#668cff", blue.getHtmlColor())
        assertTrue(blue.getColor() != -1)
        assertTrue(blue.getDescription() != -1)
    }

    @Test
    fun `test none color properties`() {
        val none = Color.None
        assertEquals(-1, none.getIndex())
        assertEquals("", none.getHtmlColor())
    }

    @Test
    fun `test getColors returns all values`() {
        val colors = Color.getColors()
        assertEquals(Color.values().size, colors.size)
    }

    private fun assertTrue(condition: Boolean) = org.junit.Assert.assertTrue(condition)
}
