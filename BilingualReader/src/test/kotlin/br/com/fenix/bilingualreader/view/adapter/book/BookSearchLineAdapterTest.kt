package br.com.fenix.bilingualreader.view.adapter.book

import br.com.fenix.bilingualreader.model.entity.BookSearch
import br.com.fenix.bilingualreader.service.listener.BookSearchListener
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
@Config(manifest = Config.NONE)
class BookSearchLineAdapterTest {

    private lateinit var adapter: BookSearchLineAdapter
    private lateinit var mockListener: BookSearchListener

    @Before
    fun setUp() {
        adapter = spyk(BookSearchLineAdapter())
        mockListener = mockk(relaxed = true)
        adapter.attachListener(mockListener)
    }

    @Test
    fun `getItemCount should return zero when list is empty`() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemCount should return size of the list when updateList is called`() {
        val list = listOf<BookSearch>(mockk(), mockk())
        
        adapter.updateList(list)
        
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemViewType should return HEADER when isTitle is true`() {
        val mockSearch = mockk<BookSearch>()
        io.mockk.every { mockSearch.isTitle } returns true
        
        adapter.updateList(listOf(mockSearch))
        
        // HEADER constant is 1
        assertEquals(1, adapter.getItemViewType(0))
    }

    @Test
    fun `getItemViewType should return CONTENT when isTitle is false`() {
        val mockSearch = mockk<BookSearch>()
        io.mockk.every { mockSearch.isTitle } returns false
        
        adapter.updateList(listOf(mockSearch))
        
        // CONTENT constant is 0
        assertEquals(0, adapter.getItemViewType(0))
    }

    @Test
    fun `notifyItemChanged should call adapter notifyItemChanged with position`() {
        val item1 = mockk<BookSearch>()
        val item2 = mockk<BookSearch>()
        val list = listOf(item1, item2)
        
        adapter.updateList(list)
        
        adapter.notifyItemChanged(mark = item2)
        
        verify { adapter.notifyItemChanged(1) }
    }
}
