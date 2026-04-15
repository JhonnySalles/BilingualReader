package br.com.fenix.bilingualreader.view.adapter.vocabulary

import android.content.Context
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.VocabularyManga
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import br.com.fenix.bilingualreader.R

@RunWith(RobolectricTestRunner::class)
@Config()
class VocabularyMangaListCardAdapterTest {

    private lateinit var context: Context
    private lateinit var adapter: VocabularyMangaListCardAdapter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.Theme_MangaReader)
        adapter = VocabularyMangaListCardAdapter()
    }

    @Test
    fun testItemCountAndCleanup() {
        val list = listOf(
            VocabularyManga(1L, 100L, 200L, 5),
            VocabularyManga(2L, 101L, 200L, 3)
        )
        adapter.updateList(list)
        assertEquals(2, adapter.itemCount)
        
        VocabularyMangaListCardAdapter.clearVocabularyMangaList()
    }

    @Test
    fun testCreateViewHolderAndBind() {
        val manga = br.com.fenix.bilingualreader.model.entity.Manga(1L, 100L, java.io.File("test.zip")).apply { title = "Test Manga" }
        val vocab = VocabularyManga(1L, 100L, 200L, 5).apply { this.manga = manga }
        adapter.updateList(listOf(vocab))
        
        val parent = FrameLayout(context)
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(1, adapter.itemCount)
    }
}
