package br.com.fenix.bilingualreader.model.entity.mock

import br.com.fenix.bilingualreader.model.entity.ShareItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import java.util.Date

object ShareItemMock {

    fun mockEntity(): ShareItem = ShareItem(
        file = "manga_item.cbz",
        bookMark = 10,
        pages = 50,
        completed = false,
        favorite = false,
        lastAccess = Date(),
        sync = Date(),
        history = mutableMapOf(),
        annotation = mutableMapOf()
    )

    fun mockEntityList(): MutableSet<ShareItem> = mutableSetOf(mockEntity())

    fun asserts(expected: ShareItem?, actual: ShareItem?) {
        assertNotNull("Actual share item should not be null", actual)
        expected?.let {
            assertEquals("File mismatch", it.file, actual?.file)
            assertEquals("BookMark mismatch", it.bookMark, actual?.bookMark)
            assertEquals("Pages mismatch", it.pages, actual?.pages)
        }
    }
}
