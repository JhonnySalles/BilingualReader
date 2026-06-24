package br.com.fenix.bilingualreader.view.adapter.configuration

import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.service.listener.LibrariesCardListener
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class LibrariesLineCardAdapterTest {

    private lateinit var adapter: LibrariesLineCardAdapter
    private lateinit var mockListener: LibrariesCardListener

    @Before
    fun setUp() {
        adapter = spyk(LibrariesLineCardAdapter())
        mockListener = mockk(relaxed = true)
        adapter.attachListener(mockListener)
    }

    @Test
    fun `getItemCount should return zero when list is empty`() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemCount should return size of the list when updateList is called`() {
        val list = mutableListOf<Library>(mockk(), mockk())
        
        adapter.updateList(list)
        
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `removeList should remove existing item from list`() {
        val lib1 = mockk<Library>()
        val lib2 = mockk<Library>()
        val list = mutableListOf(lib1, lib2)
        
        adapter.updateList(list)
        assertEquals(2, adapter.itemCount)
        
        adapter.removeList(lib1)
        
        assertEquals(1, adapter.itemCount)
        // Note: removeList in implementation doesn't call notifyItemRemoved
    }
}

