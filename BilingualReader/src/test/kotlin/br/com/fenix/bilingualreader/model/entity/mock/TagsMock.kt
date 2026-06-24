package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.Tags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object TagsMock : Mock<Long, Tags> {

    override fun mockEntity(): Tags = mockEntity(1L)

    override fun mockEntityList(): List<Tags> = listOf(
        mockEntity(1L),
        mockEntity(2L)
    )

    override fun mockEntity(id: Long): Tags = Tags(
        id = id,
        name = "Mock Tag $id",
        excluded = false,
        isSelected = false
    )

    override fun asserts(expected: Tags?, actual: Tags?) {
        assertNotNull("Actual tags should not be null", actual)
        expected?.let {
            assertEquals("ID mismatch", it.id, actual?.id)
            assertEquals("Name mismatch", it.name, actual?.name)
            assertEquals("Excluded status mismatch", it.excluded, actual?.excluded)
        }
    }
}
