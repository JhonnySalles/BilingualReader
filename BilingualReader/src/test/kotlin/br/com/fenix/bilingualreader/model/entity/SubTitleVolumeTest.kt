package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.enums.Languages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SubTitleVolumeTest {

    @Test
    fun `test subtitle volume construction`() {
        val chapters = listOf<SubTitleChapter>()
        val vocab = mutableSetOf<Vocabulary?>()
        val volume = SubTitleVolume(
            manga = "Manga",
            volume = 1.0f,
            language = Languages.JAPANESE,
            subTitleChapters = chapters,
            vocabulary = vocab
        )

        assertEquals("Manga", volume.manga)
        assertEquals(1.0f, volume.volume, 0.0f)
        assertEquals(Languages.JAPANESE, volume.language)
        assertEquals(chapters, volume.subTitleChapters)
        assertEquals(vocab, volume.vocabulary)
    }

    @Test
    fun `test subtitle volume equality`() {
        val v1 = SubTitleVolume("M", 1f, Languages.ENGLISH, listOf(), mutableSetOf())
        val v2 = SubTitleVolume("M", 1f, Languages.ENGLISH, listOf(), mutableSetOf())
        val v3 = SubTitleVolume("M", 2f, Languages.ENGLISH, listOf(), mutableSetOf())

        assertEquals(v1, v2)
        assertNotEquals(v1, v3)
        assertEquals(v1.hashCode(), v2.hashCode())
    }
}
