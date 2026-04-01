package br.com.fenix.bilingualreader.view.adapter.popup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Tags
import br.com.fenix.bilingualreader.service.listener.TagsListener
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
class TagsAdapterTest {

    private lateinit var context: Context
    private lateinit var adapter: TagsAdapter
    private lateinit var mockListener: TagsListener
    private val dataSet = mutableListOf<Tags>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockListener = mockk(relaxed = true)
        dataSet.clear()
        adapter = spyk(TagsAdapter(context, R.layout.list_line_tag, dataSet, mockListener))
    }

    @Test
    fun `getCount should return size of the list`() {
        dataSet.add(Tags(1L, "Tag 1"))
        dataSet.add(Tags(2L, "Tag 2"))
        
        assertEquals(2, adapter.count)
    }

    @Test
    fun `getItem should return tag at position`() {
        val tag = Tags(1L, "Target")
        dataSet.add(tag)
        
        assertEquals(tag, adapter.getItem(0))
    }
}
