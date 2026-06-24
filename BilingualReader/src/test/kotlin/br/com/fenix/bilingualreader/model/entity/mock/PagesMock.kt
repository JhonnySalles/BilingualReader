package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.Pages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object PagesMock {

    fun mockEntity(): Pages = Pages(
        name = "page1.jpg",
        number = 1,
        page = 1,
        isSelected = false,
        image = null
    )

    fun mockEntityList(): List<Pages> = listOf(mockEntity())

    fun asserts(expected: Pages?, actual: Pages?) {
        assertNotNull("Actual Pages should not be null", actual)
        expected?.let {
            assertEquals("Name mismatch", it.name, actual?.name)
            assertEquals("Page number mismatch", it.page, actual?.page)
        }
    }
}
