package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.Languages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class SubTitleTest {

    @Test
    fun `test subtitle construction`() {
        val now = LocalDateTime.now()
        val chapter = SubTitleChapter("Manga", 1.0f, 1.0f, Languages.JAPANESE, "Scan", listOf(), false, false, mutableSetOf())
        val subtitle = SubTitle(
            id = 1L,
            id_manga = 10L,
            language = Languages.JAPANESE,
            chapterKey = "key1",
            pageKey = "key2",
            pageCount = 100,
            path = "/path/to/file",
            dateCreate = now,
            lastAlteration = now,
            subTitleChapter = chapter,
            update = true
        )

        assertEquals(1L, subtitle.id)
        assertEquals(10L, subtitle.id_manga)
        assertEquals(Languages.JAPANESE, subtitle.language)
        assertEquals("key1", subtitle.chapterKey)
        assertEquals(100, subtitle.pageCount)
        assertEquals("/path/to/file", subtitle.path)
        assertEquals(now, subtitle.dateCreate)
        assertEquals(chapter, subtitle.subTitleChapter)
        assertTrue(subtitle.update)
    }

    @Test
    fun `test subtitle toString`() {
        val subtitle = SubTitle(language = Languages.ENGLISH, chapterKey = "K1")
        assertTrue(subtitle.toString().contains("language=ENGLISH"))
        assertTrue(subtitle.toString().contains("chapterKey='K1'"))
    }
}
