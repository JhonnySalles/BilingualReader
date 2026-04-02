package br.com.fenix.bilingualreader.view.adapter.page_link

import br.com.fenix.bilingualreader.model.entity.LinkedPage
import br.com.fenix.bilingualreader.service.listener.PageLinkCardListener
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
class PageNotLinkCardAdapterTest {

    private lateinit var adapter: PageNotLinkCardAdapter
    private lateinit var mockListener: PageLinkCardListener

    @Before
    fun setUp() {
        adapter = spyk(PageNotLinkCardAdapter())
        mockListener = mockk(relaxed = true)
        adapter.attachListener(mockListener)
    }

    @Test
    fun `getItemCount should return zero when list is empty`() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemCount should return size of the list when updateList is called`() {
        val list = arrayListOf<LinkedPage>(mockk(), mockk())
        
        adapter.updateList(list)
        
        assertEquals(2, adapter.itemCount)
    }
}
