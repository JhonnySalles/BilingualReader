package br.com.fenix.bilingualreader.view.adapter.history

import br.com.fenix.bilingualreader.model.entity.Separator
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
@Config(manifest = Config.NONE, sdk = [33])
class HistoryCardAdapterTest {

    private lateinit var adapter: HistoryLineCardAdapter
    private lateinit var mockListener: HistoryCardListener

    @Before
    fun setUp() {
        adapter = spyk(HistoryLineCardAdapter())
        mockListener = mockk(relaxed = true)
        adapter.attachListener(mockListener)
    }

    @Test
    fun `getItemCount should return zero when list is empty`() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemCount should return size of the list when updateList is called`() {
        val list = arrayListOf<Any>(mockk<History>(), mockk<History>())

        adapter.updateList(list)

        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemViewType should return HEADER when item is Separator`() {
        adapter.updateList(arrayListOf(Separator("Today")))

        // HEADER constant is 1
        assertEquals(1, adapter.getItemViewType(0))
    }

    @Test
    fun `getItemViewType should return CONTENT when item is History`() {
        val mockHistory = mockk<History>()
        every { mockHistory.id } returns 456L

        adapter.updateList(arrayListOf(mockHistory))

        // CONTENT constant is 0
        assertEquals(0, adapter.getItemViewType(0))
    }
}
