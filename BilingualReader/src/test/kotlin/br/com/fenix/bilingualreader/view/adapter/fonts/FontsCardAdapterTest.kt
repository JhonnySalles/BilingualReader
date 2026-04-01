package br.com.fenix.bilingualreader.view.adapter.fonts

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.FontType
import br.com.fenix.bilingualreader.service.listener.FontsListener
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
class FontsCardAdapterTest {

    private lateinit var context: Context
    private lateinit var adapter: FontsCardAdapter
    private lateinit var mockListener: FontsListener
    private val dataList = mutableListOf<Pair<FontType, Boolean>>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockListener = mockk(relaxed = true)
        dataList.clear()
        adapter = spyk(FontsCardAdapter(context, dataList, mockListener))
    }

    @Test
    fun `getCount should return size of the list`() {
        dataList.add(Pair(FontType.Arial, false))
        dataList.add(Pair(FontType.TimesNewRoman, true))
        
        assertEquals(2, adapter.count)
    }

    @Test
    fun `getItem should return pair at position`() {
        val pair = Pair(FontType.Arial, false)
        dataList.add(pair)
        
        assertEquals(pair, adapter.getItem(0))
    }

    @Test
    fun `updateList should change content and notify`() {
        val newList = mutableListOf(Pair(FontType.FrenchScript, true))
        
        adapter.updateList(newList)
        
        assertEquals(1, adapter.count)
        assertEquals(FontType.FrenchScript, adapter.getItem(0).first)
    }

    @Test
    fun `clicking root should trigger listener onClick`() {
        val pair = Pair(FontType.Arial, false)
        dataList.add(pair)

        val view = adapter.getView(0, null, LinearLayout(context))
        val root = view?.findViewById<LinearLayout>(R.id.font_root)

        root?.performClick()

        verify { mockListener.onClick(pair) }
    }
}
