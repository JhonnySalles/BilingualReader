package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.FileType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

object MangaMock : Mock<Long, Manga> {

    override fun mockEntity(): Manga = mockEntity(1L)

    override fun mockEntityList(): List<Manga> = listOf(
        mockEntity(1L),
        mockEntity(2L)
    )

    override fun mockEntity(id: Long): Manga = Manga(
        id = id,
        title = "Mock Manga $id",
        path = "/storage/emulated/0/BilingualReader/Manga$id.cbz",
        folder = "/storage/emulated/0/BilingualReader",
        name = "Manga$id.cbz",
        fileSize = 2048L,
        fileType = FileType.CBZ,
        pages = 50,
        chapters = intArrayOf(1, 2, 3),
        chaptersPages = mapOf(1 to "Chapter 1", 2 to "Chapter 2"),
        bookMark = 5,
        completed = false,
        favorite = false,
        hasSubtitle = true,
        author = "Mock Manga Artist",
        series = "Mock Manga Series",
        genre = "Adventure",
        publisher = "Mock Manga Publisher",
        volume = "1",
        release = LocalDate.now(),
        fkLibrary = 1L,
        excluded = false,
        dateCreate = LocalDateTime.now(),
        lastAccess = LocalDateTime.now(),
        lastAlteration = LocalDateTime.now(),
        fileAlteration = Date(),
        lastVocabImport = null,
        lastVerify = LocalDate.now()
    )

    override fun asserts(expected: Manga?, actual: Manga?) {
        assertNotNull("Actual manga should not be null", actual)
        expected?.let {
            assertEquals("ID mismatch", it.id, actual?.id)
            assertEquals("Title mismatch", it.title, actual?.title)
            assertEquals("Path mismatch", it.path, actual?.path)
            assertEquals("Pages mismatch", it.pages, actual?.pages)
            assertEquals("FK Library mismatch", it.fkLibrary, actual?.fkLibrary)
            assertEquals("Completed status mismatch", it.completed, actual?.completed)
        }
    }
}
