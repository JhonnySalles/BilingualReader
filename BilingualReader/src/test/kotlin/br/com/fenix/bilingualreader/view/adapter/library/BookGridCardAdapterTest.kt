package br.com.fenix.bilingualreader.view.adapter.library

import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.enums.LibraryBookType
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.BookCardListener
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class BookGridCardAdapterTest {

    private lateinit var adapter: BookGridCardAdapter
    private lateinit var mockListener: BookCardListener

    @Before
    fun setUp() {
        adapter = spyk(BookGridCardAdapter(LibraryBookType.LINE))
        mockListener = mockk(relaxed = true)
        adapter.attachListener(mockListener)
    }

    @Test
    fun `getItemCount should return zero when list is empty`() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `updateList should change list and notify changes`() {
        val list = mutableListOf<Book>(mockk(), mockk())
        
        adapter.updateList(Order.None, list)
        
        assertEquals(2, adapter.itemCount)
        verify { adapter.notifyItemRangeInserted(0, 2) }
    }

    @Test
    fun `removeList should remove item and notify`() {
        val book = mockk<Book>()
        adapter.updateList(Order.None, mutableListOf(book))
        
        adapter.removeList(book)
        
        assertEquals(0, adapter.itemCount)
        verify { adapter.notifyItemRemoved(0) }
    }
}
