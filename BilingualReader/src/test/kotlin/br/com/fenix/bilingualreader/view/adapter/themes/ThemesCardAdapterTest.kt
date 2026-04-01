package br.com.fenix.bilingualreader.view.adapter.themes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.service.listener.ThemesListener
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import br.com.fenix.bilingualreader.R

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ThemesCardAdapterTest {

    private lateinit var context: Context
    private lateinit var adapter: ThemesCardAdapter
    private lateinit var mockListener: ThemesListener
    private val dataList = mutableListOf<Pair<Themes, Boolean>>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockListener = mockk(relaxed = true)
        dataList.clear()
        adapter = spyk(ThemesCardAdapter(context, dataList, mockListener))
    }

    @Test
    fun `getCount should return size of the list`() {
        dataList.add(Pair(Themes.ORIGINAL, false))
        dataList.add(Pair(Themes.BLUE, true))
        
        assertEquals(2, adapter.count)
    }

    @Test
    fun `getItem should return pair at position`() {
        val pair = Pair(Themes.ORIGINAL, false)
        dataList.add(pair)
        
        assertEquals(pair, adapter.getItem(0))
    }

    @Test
    fun `updateList should change content and notify`() {
        val newList = mutableListOf(Pair(Themes.RED, true))
        
        adapter.updateList(newList)
        
        assertEquals(1, adapter.count)
    }
}
