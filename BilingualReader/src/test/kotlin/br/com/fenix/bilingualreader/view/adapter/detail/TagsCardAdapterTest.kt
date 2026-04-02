package br.com.fenix.bilingualreader.view.adapter.detail

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TagsCardAdapterTest {

    private lateinit var context: Context
    private lateinit var adapter: TagsCardAdapter
    private val dataList = mutableListOf<String>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataList.clear()
        adapter = spyk(TagsCardAdapter(context, dataList))
    }

    @Test
    fun `getCount should return size of the list`() {
        dataList.add("Tag 1")
        dataList.add("Tag 2")
        
        assertEquals(2, adapter.count)
    }

    @Test
    fun `getItem should return string at position`() {
        dataList.add("Tag 0")
        dataList.add("Target")
        
        assertEquals("Target", adapter.getItem(1))
    }

    @Test
    fun `updateList should change content and notify`() {
        val newList = mutableListOf("New Tag")
        
        adapter.updateList(newList)
        
        assertEquals(1, adapter.count)
        assertEquals("New Tag", adapter.getItem(0))
    }

    @Test
    fun `clearList should empty the list`() {
        dataList.add("Tag")
        assertEquals(1, adapter.count)
        
        adapter.clearList()
        
        assertEquals(0, adapter.count)
    }
}

