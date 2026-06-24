package br.com.fenix.bilingualreader.view.adapter.library

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.LibraryMangaType
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.MangaCardListener
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
class MangaSeparatorGridCardAdapterTest {

    private lateinit var context: Context
    private lateinit var adapter: MangaSeparatorGridCardAdapter
    private lateinit var mockListener: MangaCardListener

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        adapter = spyk(MangaSeparatorGridCardAdapter(context, LibraryMangaType.LINE))
        mockListener = mockk(relaxed = true)
        adapter.attachListener(mockListener)
    }

    @Test
    fun `getItemCount should return zero when list is empty`() {
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `updateList with Order Name should group items correctly`() {
        val manga1 = mockk<Manga>(relaxed = true)
        every { manga1.title } returns "One Piece"
        
        val manga2 = mockk<Manga>(relaxed = true)
        every { manga2.title } returns "One Punch Man"
        
        val manga3 = mockk<Manga>(relaxed = true)
        every { manga3.title } returns "Zelda"
        
        val list = mutableListOf(manga1, manga2, manga3)
        
        adapter.updateList(Order.Name, list)
        
        // Result: Separator(O), Manga1, Manga2, Separator(Z), Manga3
        assertEquals(5, adapter.itemCount)
        assertEquals(1, adapter.getItemViewType(0)) // Separator O
        assertEquals(1, adapter.getItemViewType(3)) // Separator Z
    }
}
