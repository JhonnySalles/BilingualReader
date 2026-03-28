package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

object LibraryMock : Mock<Long, Library> {

    override fun mockEntity(): Library = mockEntity(1L)

    override fun mockEntityList(): List<Library> = listOf(
        mockEntity(1L),
        mockEntity(2L)
    )

    override fun mockEntity(id: Long): Library = Library(
        id = id,
        title = "Mock Library $id",
        path = "/storage/emulated/0/BilingualReader/MockLibrary$id",
        language = Libraries.ENGLISH,
        type = Type.MANGA,
        enabled = true,
        excluded = false
    )

    override fun asserts(expected: Library?, actual: Library?) {
        assertNotNull("Actual library should not be null", actual)
        expected?.let {
            assertEquals("ID mismatch", it.id, actual?.id)
            assertEquals("Title mismatch", it.title, actual?.title)
            assertEquals("Path mismatch", it.path, actual?.path)
            assertEquals("Language mismatch", it.language, actual?.language)
            assertEquals("Type mismatch", it.type, actual?.type)
            assertEquals("Enabled status mismatch", it.enabled, actual?.enabled)
            assertEquals("Excluded status mismatch", it.excluded, actual?.excluded)
        }
    }
}
