package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.BookConfiguration
import br.com.fenix.bilingualreader.model.enums.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object BookConfigurationMock : Mock<Long, BookConfiguration> {

    override fun mockEntity(): BookConfiguration = mockEntity(1L)

    override fun mockEntityList(): List<BookConfiguration> = listOf(
        mockEntity(1L),
        mockEntity(2L)
    )

    override fun mockEntity(id: Long): BookConfiguration = BookConfiguration(
        id = id,
        idBook = 1L,
        alignment = AlignmentLayoutType.Left,
        margin = MarginLayoutType.Medium,
        spacing = SpacingLayoutType.Medium,
        fontType = FontType.Arial,
        fontSize = 18f,
        scrolling = ScrollingType.Vertical,
        pagination = PaginationType.Default
    )

    override fun asserts(expected: BookConfiguration?, actual: BookConfiguration?) {
        assertNotNull("Actual book configuration should not be null", actual)
        expected?.let {
            assertEquals("ID mismatch", it.id, actual?.id)
            assertEquals("ID Book mismatch", it.idBook, actual?.idBook)
            assertEquals("Font size mismatch", it.fontSize, actual!!.fontSize, 0.1f)
        }
    }
}
