package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.BookSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import java.time.LocalDateTime

object BookSearchMock : Mock<Long, BookSearch> {

    override fun mockEntity(): BookSearch = mockEntity(1L)

    override fun mockEntityList(): List<BookSearch> = listOf(
        mockEntity(1L),
        mockEntity(2L)
    )

    override fun mockEntity(id: Long): BookSearch = BookSearch(
        id = id,
        id_book = 1L,
        search = "Mock Search $id",
        date = LocalDateTime.now(),
        page = 1,
        chapter = 1.0f,
        isTitle = true,
        parent = null
    )

    override fun asserts(expected: BookSearch?, actual: BookSearch?) {
        assertNotNull("Actual book search should not be null", actual)
        expected?.let {
            assertEquals("ID mismatch", it.id, actual?.id)
            assertEquals("ID Book mismatch", it.id_book, actual?.id_book)
            assertEquals("Search query mismatch", it.search, actual?.search)
        }
    }
}
