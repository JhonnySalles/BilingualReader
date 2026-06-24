package br.com.fenix.bilingualreader.model.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyTest {

    @Test
    fun `test vocabulary construction`() {
        val vocab = Vocabulary(
            id = 1L,
            word = "Benkyou",
            portuguese = "Estudo",
            english = "Study",
            reading = "べんきょう",
            basicForm = "勉強",
            jlpt = 4,
            revised = true,
            favorite = true,
            appears = 10
        )

        assertEquals(1L, vocab.id)
        assertEquals("Benkyou", vocab.word)
        assertEquals("Estudo", vocab.portuguese)
        assertEquals("Study", vocab.english)
        assertEquals("べんきょう", vocab.reading)
        assertEquals("勉強", vocab.basicForm)
        assertEquals(4, vocab.jlpt)
        assertTrue(vocab.revised)
        assertTrue(vocab.favorite)
        assertEquals(10, vocab.appears)
    }

    @Test
    fun `test vocabulary equality`() {
        val v1 = Vocabulary(1L, "W", "P", "E", "R", "B", 5, false, false, 1)
        val v2 = Vocabulary(1L, "W", "P", "E", "R", "B", 5, false, false, 1)
        val v3 = Vocabulary(2L, "W", "P", "E", "R", "B", 5, false, false, 1)
        val v4 = Vocabulary(1L, "Other", "P", "E", "R", "B", 5, false, false, 1)
        val v5 = Vocabulary(1L, "W", "P", "E", "R", "Other", 5, false, false, 1)

        assertEquals("Same id, word, basicForm", v1, v2)
        assertNotEquals("Diff id", v1, v3)
        assertNotEquals("Diff word", v1, v4)
        assertNotEquals("Diff basicForm", v1, v5)
    }
}
