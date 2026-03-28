package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.SubTitle
import br.com.fenix.bilingualreader.model.enums.Languages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import java.time.LocalDateTime

object SubTitleMock : Mock<Long, SubTitle> {

    override fun mockEntity(): SubTitle = mockEntity(1L)

    override fun mockEntityList(): List<SubTitle> = listOf(
        mockEntity(1L),
        mockEntity(2L)
    )

    override fun mockEntity(id: Long): SubTitle = SubTitle(
        id = id,
        id_manga = 10L,
        language = Languages.JAPANESE,
        chapterKey = "vol1_ch1",
        pageKey = "page1",
        pageCount = 20,
        path = "/path/to/subtitle.json",
        dateCreate = LocalDateTime.now(),
        lastAlteration = LocalDateTime.now()
    )

    override fun asserts(expected: SubTitle?, actual: SubTitle?) {
        assertNotNull("Actual SubTitle should not be null", actual)
        expected?.let {
            assertEquals("ID mismatch", it.id, actual?.id)
            assertEquals("Manga ID mismatch", it.id_manga, actual?.id_manga)
            assertEquals("Language mismatch", it.language, actual?.language)
            assertEquals("Path mismatch", it.path, actual?.path)
        }
    }
}
