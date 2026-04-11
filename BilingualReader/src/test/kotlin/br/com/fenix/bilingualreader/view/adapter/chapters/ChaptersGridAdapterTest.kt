package br.com.fenix.bilingualreader.view.adapter.chapters

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import br.com.fenix.bilingualreader.model.entity.Chapters
import br.com.fenix.bilingualreader.service.listener.ChapterCardListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class ChaptersGridAdapterTest {

    private lateinit var adapter: ChaptersGridAdapter
    private lateinit var mockListener: ChapterCardListener

    @Before
    fun setUp() {
        adapter = spyk(ChaptersGridAdapter())
        mockListener = mockk(relaxed = true)
        adapter.attachListener(mockListener)
    }

    @Test
    fun `getItemCount should return zero when list is empty`() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemCount should return size of the list when updateList is called`() {
        val list = listOf<Chapters>(mockk(), mockk(), mockk())
        
        adapter.updateList(list)
        
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `getItemViewType should return HEADER when isTitle is true`() {
        val mockChapter = mockk<Chapters>()
        every { mockChapter.isTitle } returns true
        
        adapter.updateList(listOf(mockChapter))
        
        // HEADER constant is 1
        assertEquals(1, adapter.getItemViewType(0))
    }

    @Test
    fun `getItemViewType should return CONTENT when isTitle is false`() {
        val mockChapter = mockk<Chapters>()
        every { mockChapter.isTitle } returns false
        
        adapter.updateList(listOf(mockChapter))
        
        // CONTENT constant is 0
        assertEquals(0, adapter.getItemViewType(0))
    }

    @Test
    fun `onViewAttachedToWindow should set isFullSpan true for HEADER`() {
        val itemView = mockk<View>(relaxed = true)
        val lp = spyk(StaggeredGridLayoutManager.LayoutParams(100, 100))
        every { itemView.layoutParams } returns lp
        
        val holder = object : RecyclerView.ViewHolder(itemView) {}
        val field = RecyclerView.ViewHolder::class.java.getDeclaredField("mItemViewType")
        field.isAccessible = true
        field.set(holder, 1) // HEADER
        
        adapter.onViewAttachedToWindow(holder)
        
        assertTrue(lp.isFullSpan)
    }

    @Test
    fun `onViewAttachedToWindow should set isFullSpan false for CONTENT`() {
        val itemView = mockk<View>(relaxed = true)
        val lp = spyk(StaggeredGridLayoutManager.LayoutParams(100, 100))
        lp.isFullSpan = true // Start as true to verify change
        every { itemView.layoutParams } returns lp
        
        val holder = object : RecyclerView.ViewHolder(itemView) {}
        val field = RecyclerView.ViewHolder::class.java.getDeclaredField("mItemViewType")
        field.isAccessible = true
        field.set(holder, 0) // CONTENT
        
        adapter.onViewAttachedToWindow(holder)
        
        assertFalse(lp.isFullSpan)
    }
}

