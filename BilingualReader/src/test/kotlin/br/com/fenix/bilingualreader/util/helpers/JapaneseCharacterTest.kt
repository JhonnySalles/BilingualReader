package br.com.fenix.bilingualreader.util.helpers

import org.junit.Assert.*
import org.junit.Test

class JapaneseCharacterTest {

    @Test
    fun testIsKana() {
        assertTrue(JapaneseCharacter.isKana('あ'))
        assertTrue(JapaneseCharacter.isKana('ア'))
        assertFalse(JapaneseCharacter.isKana('a'))
    }

    @Test
    fun testIsHiragana() {
        assertTrue(JapaneseCharacter.isHiragana('あ'))
        assertTrue(JapaneseCharacter.isHiragana('ん'))
        assertFalse(JapaneseCharacter.isHiragana('ア'))
        assertFalse(JapaneseCharacter.isHiragana('a'))
    }

    @Test
    fun testIsKatakana() {
        assertTrue(JapaneseCharacter.isKatakana('ア'))
        assertTrue(JapaneseCharacter.isKatakana('ン'))
        assertTrue(JapaneseCharacter.isKatakana('\u30a1'))
        assertTrue(JapaneseCharacter.isKatakana('\uff66')) // Half-width
        assertFalse(JapaneseCharacter.isKatakana('あ'))
    }

    @Test
    fun testIsKanji() {
        assertTrue(JapaneseCharacter.isKanji('漢'))
        assertTrue(JapaneseCharacter.isKanji('字'))
        assertTrue(JapaneseCharacter.isKanji('\u3005')) // Iteration mark
        assertFalse(JapaneseCharacter.isKanji('あ'))
        assertFalse(JapaneseCharacter.isKanji('ア'))
    }

    @Test
    fun testIsRomaji() {
        assertTrue(JapaneseCharacter.isRomaji('a'))
        assertTrue(JapaneseCharacter.isRomaji('Z'))
        assertTrue(JapaneseCharacter.isRomaji('!'))
        assertFalse(JapaneseCharacter.isRomaji('あ'))
    }

    @Test
    fun testToKatakana() {
        assertEquals('ア', JapaneseCharacter.toKatakana('あ'))
        assertEquals('ン', JapaneseCharacter.toKatakana('ん'))
        assertEquals('a', JapaneseCharacter.toKatakana('a'))
    }

    @Test
    fun testToHiragana() {
        assertEquals('あ', JapaneseCharacter.toHiragana('ア'))
        assertEquals('ん', JapaneseCharacter.toHiragana('ン'))
        assertEquals('a', JapaneseCharacter.toHiragana('a'))
    }

    @Test
    fun testToRomaji() {
        assertEquals("a", JapaneseCharacter.toRomaji('あ'))
        assertEquals("i", JapaneseCharacter.toRomaji('い'))
        assertEquals("KA", JapaneseCharacter.toRomaji('カ'))
        assertEquals("!", JapaneseCharacter.toRomaji('!'))
    }
}
