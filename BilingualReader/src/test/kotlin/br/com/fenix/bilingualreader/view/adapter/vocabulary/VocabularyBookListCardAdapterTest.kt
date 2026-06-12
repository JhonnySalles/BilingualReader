package br.com.fenix.bilingualreader.view.adapter.vocabulary

import android.content.Context
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.VocabularyBook
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config()
class VocabularyBookListCardAdapterTest {

    private lateinit var context: Context
    private lateinit var adapter: VocabularyBookListCardAdapter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.Theme_MangaReader)
        adapter = VocabularyBookListCardAdapter()
    }

    @Test
    fun testItemCountAndCleanup() {
        val list = listOf(
            VocabularyBook(1L, 100L, 200L, 5),
            VocabularyBook(2L, 101L, 200L, 3)
        )
        adapter.updateList(list)
        assertEquals(2, adapter.itemCount)
        
        VocabularyBookListCardAdapter.clearVocabularyBookList()
        // No crash and state cleared
    }

    @Test
    fun testCreateViewHolderAndBind() {
        val book = br.com.fenix.bilingualreader.model.entity.Book(1L, 100L, java.io.File("")).apply { title = "Test Book" }
        val vocab = VocabularyBook(1L, 100L, 200L, 5).apply { this.book = book }
        adapter.updateList(listOf(vocab))
        
        val parent = FrameLayout(context)
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(1, adapter.itemCount)
    }
}
