package br.com.fenix.bilingualreader.view.adapter.reader

import android.view.View
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.service.listener.MangaAnnotationListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MangaAnnotationsCardAdapterTest {

    private lateinit var adapter: MangaAnnotationsCardAdapter
    private lateinit var mockListener: MangaAnnotationListener

    @Before
    fun setUp() {
        adapter = spyk(MangaAnnotationsCardAdapter())
        mockListener = mockk(relaxed = true)
        adapter.attachListener(mockListener)
    }

    @Test
    fun `getItemCount should return zero when list is empty`() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemCount should return size of the list when updateList is called`() {
        val mock1 = mockk<MangaAnnotation>()
        every { mock1.isTitle } returns true
        val mock2 = mockk<MangaAnnotation>()
        every { mock2.isTitle } returns false
        val list = listOf<MangaAnnotation>(mock1, mock2)
        
        adapter.updateList(list)
        
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemViewType should return HEADER when isTitle is true`() {
        val mockAnnotation = mockk<MangaAnnotation>()
        every { mockAnnotation.isTitle } returns true
        
        adapter.updateList(listOf(mockAnnotation))
        
        // HEADER constant is 1
        assertEquals(1, adapter.getItemViewType(0))
    }

    @Test
    fun `onViewAttachedToWindow should set isFullSpan true for HEADER`() {
        val holder = mockk<MangaAnnotationHeaderViewHolder>(relaxed = true)
        val itemView = mockk<View>(relaxed = true)
        val lp = spyk(StaggeredGridLayoutManager.LayoutParams(100, 100))
        
        every { holder.itemView } returns itemView
        every { itemView.layoutParams } returns lp
        every { holder.itemViewType } returns 1 // HEADER
        
        adapter.onViewAttachedToWindow(holder)
        
        assertTrue(lp.isFullSpan)
    }
}
