package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.Separator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object SeparatorMock {

    fun mockEntity(): Separator = Separator(
        title = "Mock Separator",
        items = 5
    )

    fun mockEntityList(): List<Separator> = listOf(
        mockEntity()
    )

    fun asserts(expected: Separator?, actual: Separator?) {
        assertNotNull("Actual separator should not be null", actual)
        expected?.let {
            assertEquals("Title mismatch", it.title, actual?.title)
            assertEquals("Items count mismatch", it.items, actual?.items)
        }
    }
}
