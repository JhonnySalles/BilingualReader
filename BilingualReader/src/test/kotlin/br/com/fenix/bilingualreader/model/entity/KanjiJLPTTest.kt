package br.com.fenix.bilingualreader.model.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KanjiJLPTTest {

    @Test
    fun `test kanji jlpt construction`() {
        val kanji = KanjiJLPT(id = 1L, kanji = "日", level = 5)
        
        assertEquals(1L, kanji.id)
        assertEquals("日", kanji.kanji)
        assertEquals(5, kanji.level)
    }

    @Test
    fun `test kanji jlpt toString`() {
        val kanji = KanjiJLPT(kanji = "月", level = 4)
        assertTrue(kanji.toString().contains("kanji='月'"))
        assertTrue(kanji.toString().contains("leve='4'"))
    }
}
