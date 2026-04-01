package br.com.fenix.bilingualreader.view.adapter.library

import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.MangaCardListener
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
class MangaLineCardAdapterTest {

    private lateinit var adapter: MangaLineCardAdapter
    private lateinit var mockListener: MangaCardListener

    @Before
    fun setUp() {
        adapter = spyk(MangaLineCardAdapter())
        mockListener = mockk(relaxed = true)
        adapter.attachListener(mockListener)
    }

    @Test
    fun `getItemCount should return zero when list is empty`() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `updateList should change list and notify`() {
        val list = mutableListOf<Manga>(mockk(), mockk())
        
        adapter.updateList(Order.None, list)
        
        assertEquals(2, adapter.itemCount)
        verify { adapter.notifyItemRangeInserted(0, 2) }
    }

    @Test
    fun `removeList should remove item and notify`() {
        val manga = mockk<Manga>()
        adapter.updateList(Order.None, mutableListOf(manga))
        
        adapter.removeList(manga)
        
        assertEquals(0, adapter.itemCount)
        verify { adapter.notifyItemRemoved(0) }
    }
}
