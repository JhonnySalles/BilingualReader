package br.com.fenix.bilingualreader.model.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class SubTitlePageTest {

    @Test
    fun `test subtitle page construction`() {
        val texts = listOf<SubTitleText>()
        val vocab = mutableSetOf<Vocabulary>()
        val page = SubTitlePage(
            name = "Page01",
            number = 1,
            hash = "abc",
            subTitleTexts = texts,
            vocabulary = vocab
        )

        assertEquals("Page01", page.name)
        assertEquals(1, page.number)
        assertEquals("abc", page.hash)
        assertEquals(texts, page.subTitleTexts)
        assertEquals(vocab, page.vocabulary)
    }
}
