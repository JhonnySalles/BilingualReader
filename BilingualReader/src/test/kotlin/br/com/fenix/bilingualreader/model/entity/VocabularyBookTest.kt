package br.com.fenix.bilingualreader.model.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class VocabularyBookTest {

    @Test
    fun `test vocabulary book construction`() {
        val vb = VocabularyBook(
            id = 1L,
            idVocabulary = 10L,
            idBook = 20L,
            appears = 5
        )

        assertEquals(1L, vb.id)
        assertEquals(10L, vb.idVocabulary)
        assertEquals(20L, vb.idBook)
        assertEquals(5, vb.appears)
    }
}
