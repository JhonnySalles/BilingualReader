package br.com.fenix.bilingualreader.view.adapter.annotation

import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.model.interfaces.Annotation
import br.com.fenix.bilingualreader.service.listener.AnnotationsListener
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AnnotationLineAdapterTest {

    private lateinit var adapter: AnnotationLineAdapter
    private lateinit var mockListener: AnnotationsListener

    @Before
    fun setUp() {
        adapter = spyk(AnnotationLineAdapter())
        mockListener = mockk(relaxed = true)
        adapter.attachListener(mockListener)
    }

    @Test
    fun `getItemCount should return zero when list is empty`() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemCount should return size of the list when updateList is called`() {
        val list = mutableListOf<Annotation>()
        repeat(5) { list.add(mockk()) }
        
        adapter.updateList(list)
        
        assertEquals(5, adapter.itemCount)
    }

    @Test
    fun `getItemViewType should return ROOT when annotation isRoot is true`() {
        val mockAnnotation = mockk<Annotation>()
        every { mockAnnotation.isRoot } returns true
        
        adapter.updateList(mutableListOf(mockAnnotation))
        
        // ROOT constant is 2 (from companion object)
        assertEquals(2, adapter.getItemViewType(0))
    }

    @Test
    fun `getItemViewType should return TITLE when annotation isTitle is true`() {
        val mockAnnotation = mockk<Annotation>()
        every { mockAnnotation.isRoot } returns false
        every { mockAnnotation.isTitle } returns true
        
        adapter.updateList(mutableListOf(mockAnnotation))
        
        // TITLE constant is 1
        assertEquals(1, adapter.getItemViewType(0))
    }

    @Test
    fun `getItemViewType should return BOOK when type is BOOK`() {
        val mockAnnotation = mockk<Annotation>()
        every { mockAnnotation.isRoot } returns false
        every { mockAnnotation.isTitle } returns false
        every { mockAnnotation.type } returns Type.BOOK
        
        adapter.updateList(mutableListOf(mockAnnotation))
        
        // BOOK constant is 3
        assertEquals(3, adapter.getItemViewType(0))
    }

    @Test
    fun `getItemViewType should return MANGA when type is MANGA`() {
        val mockAnnotation = mockk<Annotation>()
        every { mockAnnotation.isRoot } returns false
        every { mockAnnotation.isTitle } returns false
        every { mockAnnotation.type } returns Type.MANGA
        
        adapter.updateList(mutableListOf(mockAnnotation))
        
        // MANGA constant is 4
        assertEquals(4, adapter.getItemViewType(0))
    }

    @Test
    fun `notifyItemChanged(Annotation) should call notifyItemChanged(Int) when item exists`() {
        val item1 = mockk<Annotation>()
        val item2 = mockk<Annotation>()
        val list = mutableListOf(item1, item2)
        
        adapter.updateList(list)
        
        // Capture notifyItemChanged call, being explicit with named parameter to avoid ambiguity
        adapter.notifyItemChanged(item = item2)
        
        // Verify notifyItemChanged(Int) was called with index 1
        // We use position = 1 if possible, or just the value
        verify { adapter.notifyItemChanged(1) }
    }

    @Test
    fun `notifyItemChanged(Annotation) should not call notifyItemChanged(Int) when item does not exist`() {
        val item1 = mockk<Annotation>()
        val item2 = mockk<Annotation>()
        val list = mutableListOf(item1)
        
        adapter.updateList(list)
        
        adapter.notifyItemChanged(item = item2)
        
        verify(exactly = 0) { adapter.notifyItemChanged(any<Int>()) }
    }
}
