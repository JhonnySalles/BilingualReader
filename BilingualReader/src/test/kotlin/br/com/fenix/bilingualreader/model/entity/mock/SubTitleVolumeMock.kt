package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.SubTitleVolume
import br.com.fenix.bilingualreader.model.enums.Languages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object SubTitleVolumeMock {

    fun mockEntity(): SubTitleVolume = SubTitleVolume(
        manga = "Mock Manga",
        volume = 1.0f,
        language = Languages.JAPANESE,
        subTitleChapters = listOf(SubTitleChapterMock.mockEntity()),
        vocabulary = mutableSetOf()
    )

    fun mockEntityList(): List<SubTitleVolume> = listOf(mockEntity())

    fun asserts(expected: SubTitleVolume?, actual: SubTitleVolume?) {
        assertNotNull("Actual SubTitleVolume should not be null", actual)
        expected?.let {
            assertEquals("Manga title mismatch", it.manga, actual?.manga)
            assertEquals("Volume mismatch", it.volume, actual!!.volume, 0.01f)
            assertEquals("Language mismatch", it.language, actual?.language)
        }
    }
}
