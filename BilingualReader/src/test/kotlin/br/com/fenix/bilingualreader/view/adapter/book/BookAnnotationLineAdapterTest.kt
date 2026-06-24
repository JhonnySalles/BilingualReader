package br.com.fenix.bilingualreader.view.adapter.book

import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.service.listener.AnnotationsListener
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
@Config(sdk = [33], manifest = Config.NONE)
class BookAnnotationLineAdapterTest {

    private lateinit var adapter: BookAnnotationLineAdapter
    private lateinit var mockListener: AnnotationsListener

    @Before
    fun setUp() {
        adapter = spyk(BookAnnotationLineAdapter())
        mockListener = mockk(relaxed = true)
        adapter.attachListener(mockListener)
    }

    @Test
    fun `getItemCount should return zero when list is empty`() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemCount should return size of the list when updateList is called`() {
        val list = mutableListOf<BookAnnotation>()
        repeat(3) { list.add(mockk()) }
        
        adapter.updateList(list)
        
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `getItemViewType should return HEADER when id is null`() {
        val mockAnnotation = mockk<BookAnnotation>()
        io.mockk.every { mockAnnotation.id } returns null
        
        adapter.updateList(mutableListOf(mockAnnotation))
        
        // HEADER constant is 1
        assertEquals(1, adapter.getItemViewType(0))
    }

    @Test
    fun `getItemViewType should return CONTENT when id is not null`() {
        val mockAnnotation = mockk<BookAnnotation>()
        io.mockk.every { mockAnnotation.id } returns 123L
        
        adapter.updateList(mutableListOf(mockAnnotation))
        
        // CONTENT constant is 0
        assertEquals(0, adapter.getItemViewType(0))
    }

    @Test
    fun `notifyItemChanged should call adapter notifyItemChanged with position`() {
        val item1 = mockk<BookAnnotation>()
        val item2 = mockk<BookAnnotation>()
        val list = mutableListOf(item1, item2)
        
        adapter.updateList(list)
        
        adapter.notifyItemChanged(annotation = item2)
        
        verify { adapter.notifyItemChanged(1) }
    }
}

