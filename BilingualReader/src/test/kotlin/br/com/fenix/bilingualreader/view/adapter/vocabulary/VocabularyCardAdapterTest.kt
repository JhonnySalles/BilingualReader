package br.com.fenix.bilingualreader.view.adapter.vocabulary

import br.com.fenix.bilingualreader.model.entity.Vocabulary
import br.com.fenix.bilingualreader.service.listener.VocabularyCardListener
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import androidx.recyclerview.widget.DiffUtil
import java.lang.reflect.Field

class VocabularyCardAdapterTest {

    private lateinit var adapter: VocabularyCardAdapter
    private lateinit var mockListener: VocabularyCardListener

    @Before
    fun setUp() {
        mockListener = mockk(relaxed = true)
        adapter = spyk(VocabularyCardAdapter(mockListener))
    }

    @Suppress("UNCHECKED_CAST")
    private fun getDiffCallback(): DiffUtil.ItemCallback<Vocabulary> {
        val companion = VocabularyCardAdapter.Companion
        val field: Field = VocabularyCardAdapter.Companion::class.java.getDeclaredField("DIFF_CALLBACK")
        field.isAccessible = true
        return field.get(companion) as DiffUtil.ItemCallback<Vocabulary>
    }

    @Test
    fun `areItemsTheSame should return true for same id`() {
        val diffCallback = getDiffCallback()
        val oldItem = Vocabulary(1L, "Word", null, null, null, null, 0, false, false, 0)
        val newItem = Vocabulary(1L, "Word", null, null, null, null, 0, false, false, 0)
        
        assertTrue(diffCallback.areItemsTheSame(oldItem, newItem))
    }

    @Test
    fun `areItemsTheSame should return false for different id`() {
        val diffCallback = getDiffCallback()
        val oldItem = Vocabulary(1L, "Word", null, null, null, null, 0, false, false, 0)
        val newItem = Vocabulary(2L, "Word", null, null, null, null, 0, false, false, 0)
        
        assertFalse(diffCallback.areItemsTheSame(oldItem, newItem))
    }
}
