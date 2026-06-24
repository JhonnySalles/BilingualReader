package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.LinkedFile
import br.com.fenix.bilingualreader.model.enums.Languages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import java.time.LocalDateTime

object LinkedFileMock : Mock<Long, LinkedFile> {

    override fun mockEntity(): LinkedFile = mockEntity(1L)

    override fun mockEntityList(): List<LinkedFile> = listOf(
        mockEntity(1L),
        mockEntity(2L)
    )

    override fun mockEntity(id: Long): LinkedFile = LinkedFile(
        id = id,
        idManga = 10L,
        pages = 100,
        path = "/path/to/file.cbz",
        name = "Manga File",
        type = "CBZ",
        folder = "Manga Folder",
        language = Languages.JAPANESE,
        dateCreate = LocalDateTime.now(),
        lastAccess = LocalDateTime.now(),
        lastAlteration = LocalDateTime.now()
    )

    override fun asserts(expected: LinkedFile?, actual: LinkedFile?) {
        assertNotNull("Actual LinkedFile should not be null", actual)
        expected?.let {
            assertEquals("ID mismatch", it.id, actual?.id)
            assertEquals("Manga ID mismatch", it.idManga, actual?.idManga)
            assertEquals("File name mismatch", it.name, actual?.name)
            assertEquals("File type mismatch", it.type, actual?.type)
        }
    }
}
