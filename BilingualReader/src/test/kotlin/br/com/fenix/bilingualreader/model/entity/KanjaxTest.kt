package br.com.fenix.bilingualreader.model.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KanjaxTest {

    @Test
    fun `test kanjax construction`() {
        val kanjax = Kanjax(
            id = 1L,
            kanji = "学",
            keyword = "Study",
            meaning = "Study; learning; science",
            koohii = "Story 1",
            koohii2 = "Story 2",
            onYomi = "ガク",
            kunYomi = "まな-ぶ",
            onWords = "学校",
            kunWords = "学ぶ",
            jlpt = 4,
            grade = 1,
            frequence = 100,
            strokes = 8,
            variants = "variant",
            radical = "子",
            parts = "parts",
            utf8 = "E5ADA6",
            sjis = "sjis",
            keywordPt = "Estudar",
            meaningPt = "Estudo"
        )

        assertEquals(1L, kanjax.id)
        assertEquals("学", kanjax.kanji)
        assertEquals("Study", kanjax.keyword)
        assertEquals("Estudo", kanjax.meaningPt)
        assertEquals(4, kanjax.jlpt)
    }

    @Test
    fun `test kanjax toString`() {
        val kanjax = Kanjax(
            kanji = "文", keyword = "Sentence", meaning = "sentence", koohii = "", koohii2 = "",
            onYomi = "", kunYomi = "", onWords = "", kunWords = "", jlpt = 4, grade = 1,
            frequence = 1, strokes = 4, variants = "", radical = "", parts = "",
            utf8 = "", sjis = "", keywordPt = "", meaningPt = ""
        )
        assertTrue(kanjax.toString().contains("kanji='文'"))
        assertTrue(kanjax.toString().contains("keyword='Sentence'"))
    }
}
