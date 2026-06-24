package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.model.enums.MarkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import java.time.LocalDateTime

object MangaAnnotationMock : Mock<Long, MangaAnnotation> {

    override fun mockEntity(): MangaAnnotation = mockEntity(1L)

    override fun mockEntityList(): List<MangaAnnotation> = listOf(
        mockEntity(1L),
        mockEntity(2L)
    )

    override fun mockEntity(id: Long): MangaAnnotation = MangaAnnotation(
        id = id,
        id_parent = 20L,
        page = 10,
        pages = 50,
        markType = MarkType.PageMark,
        chapter = "Chapter 2",
        folder = "/path/to/manga",
        annotation = "Mocked manga annotation",
        alteration = LocalDateTime.now(),
        created = LocalDateTime.now()
    )

    override fun asserts(expected: MangaAnnotation?, actual: MangaAnnotation?) {
        assertNotNull("Actual manga annotation should not be null", actual)
        expected?.let {
            assertEquals("ID mismatch", it.id, actual?.id)
            assertEquals("Parent ID mismatch", it.id_parent, actual?.id_parent)
            assertEquals("Page mismatch", it.page, actual?.page)
            assertEquals("Annotation content mismatch", it.annotation, actual?.annotation)
        }
    }
}
