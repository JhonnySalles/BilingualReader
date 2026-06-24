package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.MarkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import java.time.LocalDateTime

object BookAnnotationMock : Mock<Long, BookAnnotation> {

    override fun mockEntity(): BookAnnotation = mockEntity(1L)

    override fun mockEntityList(): List<BookAnnotation> = listOf(
        mockEntity(1L),
        mockEntity(2L)
    )

    override fun mockEntity(id: Long): BookAnnotation = BookAnnotation(
        id = id,
        id_parent = 10L,
        page = 5,
        pages = 100,
        fontSize = 18f,
        markType = MarkType.Annotation,
        chapterNumber = 1.0f,
        chapter = "Chapter 1",
        text = "Mocked text for annotation",
        range = intArrayOf(0, 10),
        annotation = "Mocked annotation note",
        favorite = false,
        color = Color.Yellow,
        alteration = LocalDateTime.now(),
        created = LocalDateTime.now()
    )

    override fun asserts(expected: BookAnnotation?, actual: BookAnnotation?) {
        assertNotNull("Actual book annotation should not be null", actual)
        expected?.let {
            assertEquals("ID mismatch", it.id, actual?.id)
            assertEquals("Parent ID mismatch", it.id_parent, actual?.id_parent)
            assertEquals("Page mismatch", it.page, actual?.page)
            assertEquals("Annotation text mismatch", it.annotation, actual?.annotation)
        }
    }
}
