package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Languages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

object BookMock : Mock<Long, Book> {

    override fun mockEntity(): Book = mockEntity(1L)

    override fun mockEntityList(): List<Book> = listOf(
        mockEntity(1L),
        mockEntity(2L)
    )

    override fun mockEntity(id: Long): Book = Book(
        id = id,
        title = "Mock Book $id",
        author = "Mock Author $id",
        password = "",
        annotation = "Mock Annotation $id",
        release = LocalDate.now(),
        genre = "Action",
        publisher = "Mock Publisher",
        series = "Mock Series",
        isbn = "1234567890",
        pages = 100,
        volume = "1",
        chapter = 1,
        chapterDescription = "Chapter 1",
        bookMark = 10,
        completed = false,
        language = Languages.ENGLISH,
        path = "/storage/emulated/0/BilingualReader/Book$id.epub",
        folder = "/storage/emulated/0/BilingualReader",
        name = "Book$id.epub",
        fileType = FileType.EPUB,
        fileSize = 1024L,
        favorite = false,
        fkLibrary = 1L,
        tags = mutableListOf(),
        excluded = false,
        dateCreate = LocalDateTime.now(),
        lastAccess = LocalDateTime.now(),
        lastAlteration = LocalDateTime.now(),
        fileAlteration = Date(),
        lastVocabImport = null,
        lastVerify = LocalDate.now()
    )

    override fun asserts(expected: Book?, actual: Book?) {
        assertNotNull("Actual book should not be null", actual)
        expected?.let {
            assertEquals("ID mismatch", it.id, actual?.id)
            assertEquals("Title mismatch", it.title, actual?.title)
            assertEquals("Author mismatch", it.author, actual?.author)
            assertEquals("Path mismatch", it.path, actual?.path)
            assertEquals("Language mismatch", it.language, actual?.language)
            assertEquals("FK Library mismatch", it.fkLibrary, actual?.fkLibrary)
            assertEquals("Pages mismatch", it.pages, actual?.pages)
            assertEquals("Completed status mismatch", it.completed, actual?.completed)
        }
    }
}
