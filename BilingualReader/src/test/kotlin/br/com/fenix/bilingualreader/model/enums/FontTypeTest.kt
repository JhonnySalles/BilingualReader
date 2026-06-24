package br.com.fenix.bilingualreader.model.enums

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FontTypeTest {

    @Test
    fun `test font properties`() {
        val font = FontType.TimesNewRoman
        assertEquals("times_new_roman.ttf", font.getName())
        assertEquals(false, font.isJapanese())
        
        val japaneseFont = FontType.BabelStoneHan
        assertEquals(true, japaneseFont.isJapanese())
        assertTrue(japaneseFont.getFontRotate() != 0)
    }

    @Test
    fun `test getCssFont generates correct font-face rules`() {
        val css = FontType.getCssFont()
        
        // Should contain all fonts
        FontType.values().forEach {
            assertTrue("CSS should contain ${it.name}", css.contains("font-family: ${it.name}"))
            assertTrue("CSS should contain ${it.getName()}", css.contains("url(\"font/${it.getName()}\")"))
        }
        
        // Check pattern of one rule
        assertTrue(css.contains("@font-face { font-family: TimesNewRoman; src: url(\"font/times_new_roman.ttf\"); }"))
    }
}
