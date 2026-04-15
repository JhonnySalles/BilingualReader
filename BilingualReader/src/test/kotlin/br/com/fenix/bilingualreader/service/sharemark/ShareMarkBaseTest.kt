package br.com.fenix.bilingualreader.service.sharemark

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.ShareItem
import br.com.fenix.bilingualreader.model.enums.ShareMarkType
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.util.*

class ShareMarkBaseTest {

    private val context = mockk<Context>()
    
    // Concrete implementation for testing abstract class
    private class TestShareMark(context: Context) : ShareMarkBase(context) {
        override val mNotConnectErrorType: ShareMarkType = ShareMarkType.NOT_CONNECT_DRIVE
        override fun initialize(ending: (access: ShareMarkType) -> Unit) {}
        override fun processManga(update: (manga: Manga) -> Unit, ending: (processed: ShareMarkType) -> Unit) {}
        override fun processBook(update: (book: Book) -> Unit, ending: (processed: ShareMarkType) -> Unit) {}
        
        // Expose protected methods for testing
        fun testCompareManga(item: ShareItem, manga: Manga) = compare(item, manga)
        fun testCompareBook(item: ShareItem, book: Book) = compare(item, book)
    }

    private lateinit var shareMark: TestShareMark

    @Before
    fun setup() {
        shareMark = TestShareMark(context)
    }

    @Test
    fun `compare manga should update manga if item is newer`() {
        // Initializing with secondary constructor: Manga(fkLibrary, id, file)
        val manga = Manga(null, 1L, File("path"))
        manga.lastAccess = LocalDateTime.now().minusDays(1)
        
        // ShareItem(file, bookMark, pages, completed, favorite, lastAccess)
        val item = ShareItem("Manga", 50, 100, false, true, Date())
        
        val result = shareMark.testCompareManga(item, manga)
        
        assertTrue(result)
        assertEquals(50, manga.bookMark)
        assertTrue(item.processed)
        assertTrue(item.received)
    }

    @Test
    fun `compare book should calculate bookmark percentage if pages differ`() {
        // Initializing with secondary constructor: Book(fkLibrary, id, file)
        val book = Book(null, 1L, File("path")) 
        book.pages = 200
        book.lastAccess = LocalDateTime.now().minusDays(1)
        
        // Cloud has 100 pages, bookmark at 50 (50%)
        val item = ShareItem("Book", 50, 100, false, true, Date())
        
        val result = shareMark.testCompareBook(item, book)
        
        assertTrue(result)
        assertEquals(100, book.bookMark) // 50% of 200 is 100
    }
}
