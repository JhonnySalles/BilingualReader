package br.com.fenix.bilingualreader.view.adapter.library

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Separator
import br.com.fenix.bilingualreader.model.enums.LibraryBookType
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.BookCardListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BookSeparatorGridCardAdapterTest {

    private lateinit var context: Context
    private lateinit var adapter: BookSeparatorGridCardAdapter
    private lateinit var mockListener: BookCardListener

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        adapter = spyk(BookSeparatorGridCardAdapter(context, LibraryBookType.LINE))
        mockListener = mockk(relaxed = true)
        adapter.attachListener(mockListener)
    }

    @Test
    fun `getItemCount should return zero when list is empty`() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `updateList with Order Name should group items correctly`() {
        // Book 1: Title "Apple"
        val book1 = mockk<Book>(relaxed = true)
        every { book1.title } returns "Apple"
        
        // Book 2: Title "Banana"
        val book2 = mockk<Book>(relaxed = true)
        every { book2.title } returns "Banana"
        
        val list = mutableListOf(book1, book2)
        
        adapter.updateList(Order.Name, list)
        
        // Result should be: Separator(A), Book1, Separator(B), Book2
        assertEquals(4, adapter.itemCount)
        
        // HEADER constant is 1 (for Separator), CONTENT constant is 0 (for Book)
        assertEquals(1, adapter.getItemViewType(0)) // Separator A
        assertEquals(0, adapter.getItemViewType(1)) // Book Apple
        assertEquals(1, adapter.getItemViewType(2)) // Separator B
        assertEquals(0, adapter.getItemViewType(3)) // Book Banana
    }

    @Test
    fun `updateList with same starting letter should group under one separator`() {
        val book1 = mockk<Book>(relaxed = true)
        every { book1.title } returns "Apple"
        
        val book2 = mockk<Book>(relaxed = true)
        every { book2.title } returns "Ant"
        
        val list = mutableListOf(book1, book2)
        
        adapter.updateList(Order.Name, list)
        
        // Result should be: Separator(A), Book1, Book2
        assertEquals(3, adapter.itemCount)
        assertEquals(1, adapter.getItemViewType(0)) // Separator A
        assertEquals(0, adapter.getItemViewType(1)) // Book Apple
        assertEquals(0, adapter.getItemViewType(2)) // Book Ant
    }

    @Test
    fun `updateList with Order None should not add separators`() {
        val book = mockk<Book>(relaxed = true)
        val list = mutableListOf(book)
        
        adapter.updateList(Order.None, list)
        
        assertEquals(1, adapter.itemCount)
        assertEquals(0, adapter.getItemViewType(0))
    }
}
