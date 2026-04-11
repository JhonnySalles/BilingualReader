package br.com.fenix.bilingualreader.view.adapter.popup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Kanjax
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
class VocabularyKanjiAdapterTest {

    private lateinit var context: Context
    private lateinit var adapter: VocabularyKanjiAdapter
    private val dataSet = mutableListOf<Kanjax>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataSet.clear()
        adapter = spyk(VocabularyKanjiAdapter(context, R.layout.list_line_kanji, dataSet))
    }

    @Test
    fun `getCount should return size of the list`() {
        dataSet.add(mockk())
        dataSet.add(mockk())
        
        assertEquals(2, adapter.count)
    }

    @Test
    fun `getItem should return kanjax at position`() {
        val kanjax = mockk<Kanjax>()
        dataSet.add(kanjax)
        
        assertEquals(kanjax, adapter.getItem(0))
    }
}
