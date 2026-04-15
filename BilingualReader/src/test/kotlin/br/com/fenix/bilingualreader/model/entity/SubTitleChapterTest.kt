package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.Languages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SubTitleChapterTest {

    @Test
    fun `test subtitle chapter construction`() {
        val pages = listOf<SubTitlePage>()
        val vocab = mutableSetOf<Vocabulary>()
        val chapter = SubTitleChapter(
            manga = "Manga",
            volume = 1.0f,
            chapter = 1.5f,
            language = Languages.PORTUGUESE,
            scan = "ScanGroup",
            subTitlePages = pages,
            extra = true,
            raw = false,
            vocabulary = vocab
        )

        assertEquals("Manga", chapter.manga)
        assertEquals(1.0f, chapter.volume, 0.0f)
        assertEquals(1.5f, chapter.chapter, 0.0f)
        assertEquals(Languages.PORTUGUESE, chapter.language)
        assertEquals(pages, chapter.subTitlePages)
        assertEquals(vocab, chapter.vocabulary)
    }

    @Test
    fun `test subtitle chapter equality`() {
        val c1 = SubTitleChapter("M", 1f, 1f, Languages.ENGLISH, "S", listOf(), false, false, mutableSetOf())
        val c2 = SubTitleChapter("M", 1f, 1f, Languages.ENGLISH, "S", listOf(), false, false, mutableSetOf())
        val c3 = SubTitleChapter("Other", 1f, 1f, Languages.ENGLISH, "S", listOf(), false, false, mutableSetOf())

        assertEquals(c1, c2)
        assertNotEquals(c1, c3)
        assertEquals(c1.hashCode(), c2.hashCode())
    }
}
