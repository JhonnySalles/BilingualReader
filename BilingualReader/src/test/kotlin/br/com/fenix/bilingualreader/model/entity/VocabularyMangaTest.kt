package br.com.fenix.bilingualreader.model.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class VocabularyMangaTest {

    @Test
    fun `test vocabulary manga construction`() {
        val vm = VocabularyManga(
            id = 1L,
            idVocabulary = 10L,
            idManga = 20L,
            appears = 5
        )

        assertEquals(1L, vm.id)
        assertEquals(10L, vm.idVocabulary)
        assertEquals(20L, vm.idManga)
        assertEquals(5, vm.appears)
    }
}
