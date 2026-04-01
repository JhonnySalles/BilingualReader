package br.com.fenix.bilingualreader.view.adapter.history

import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.listener.HistoryCardListener
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
class HistoryCardAdapterTest {

    private lateinit var adapter: HistoryCardAdapter
    private lateinit var mockListener: HistoryCardListener

    @Before
    fun setUp() {
        adapter = spyk(HistoryCardAdapter())
        mockListener = mockk(relaxed = true)
        adapter.attachListener(mockListener)
    }

    @Test
    fun `getItemCount should return zero when list is empty`() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemCount should return size of the list when updateList is called`() {
        val list = arrayListOf<History>(mockk(), mockk())
        
        adapter.updateList(list)
        
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemViewType should return HEADER when id is null`() {
        val mockHistory = mockk<History>()
        every { mockHistory.id } returns null
        
        adapter.updateList(arrayListOf(mockHistory))
        
        // HEADER constant is 1
        assertEquals(1, adapter.getItemViewType(0))
    }

    @Test
    fun `getItemViewType should return CONTENT when id is not null`() {
        val mockHistory = mockk<History>()
        every { mockHistory.id } returns 456L
        
        adapter.updateList(arrayListOf(mockHistory))
        
        // CONTENT constant is 0
        assertEquals(0, adapter.getItemViewType(0))
    }
}
