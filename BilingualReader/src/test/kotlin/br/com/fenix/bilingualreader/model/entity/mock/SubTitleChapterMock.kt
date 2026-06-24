package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.*
import br.com.fenix.bilingualreader.model.enums.Languages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object SubTitleChapterMock {

    fun mockEntity(): SubTitleChapter = SubTitleChapter(
        manga = "Mock Manga",
        volume = 1.0f,
        chapter = 1.5f,
        language = Languages.JAPANESE,
        scan = "Mock Scan",
        subTitlePages = listOf(SubTitlePageMock.mockEntity()),
        extra = false,
        raw = false,
        vocabulary = mutableSetOf()
    )

    fun mockEntityList(): List<SubTitleChapter> = listOf(mockEntity())

    fun asserts(expected: SubTitleChapter?, actual: SubTitleChapter?) {
        assertNotNull("Actual SubTitleChapter should not be null", actual)
        expected?.let {
            assertEquals("Manga title mismatch", it.manga, actual?.manga)
            assertEquals("Volume mismatch", it.volume, actual!!.volume, 0.01f)
            assertEquals("Chapter mismatch", it.chapter, actual!!.chapter, 0.01f)
            assertEquals("Language mismatch", it.language, actual?.language)
        }
    }
}
