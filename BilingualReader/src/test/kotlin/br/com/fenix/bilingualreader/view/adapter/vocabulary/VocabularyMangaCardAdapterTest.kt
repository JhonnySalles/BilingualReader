package br.com.fenix.bilingualreader.view.adapter.vocabulary

import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.service.listener.VocabularyCardListener
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VocabularyMangaCardAdapterTest {

    private lateinit var adapter: VocabularyMangaCardAdapter
    private lateinit var mockListener: VocabularyCardListener

    @Before
    fun setUp() {
        mockListener = mockk(relaxed = true)
        adapter = spyk(VocabularyMangaCardAdapter(mockListener))
    }

    @Test
    fun `areItemsTheSame should return true for same id`() {
        val diffCallback = VocabularyMangaCardAdapter.DIFF_CALLBACK
        val oldItem = Vocabulary(1L, "Word", null, null, null, null, 0, false, false, 0)
        val newItem = Vocabulary(1L, "Word", null, null, null, null, 0, false, false, 0)
        
        assertTrue(diffCallback.areItemsTheSame(oldItem, newItem))
    }
}
