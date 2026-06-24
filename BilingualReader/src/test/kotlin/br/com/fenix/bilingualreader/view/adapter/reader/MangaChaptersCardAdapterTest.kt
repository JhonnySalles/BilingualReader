package br.com.fenix.bilingualreader.view.adapter.reader

import android.content.Context
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Chapters
import br.com.fenix.bilingualreader.service.listener.ChapterCardListener
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config()
class MangaChaptersCardAdapterTest {

    private lateinit var context: Context
    private lateinit var adapter: MangaChaptersCardAdapter
    private val listener: ChapterCardListener = mockk(relaxed = true)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.Theme_MangaReader)
        adapter = MangaChaptersCardAdapter()
        adapter.attachListener(listener)
    }

    @Test
    fun testItemCount() {
        val list = listOf(
            Chapters("Ch 1", 1, 1, 1f, false),
            Chapters("Ch 2", 2, 2, 2f, false)
        )
        adapter.updateList(list)
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun testOnBindViewHolder() {
        val chapter = Chapters("Ch 1", 1, 1, 1f, false).apply { isSelected = true }
        adapter.updateList(listOf(chapter))
        
        val parent = FrameLayout(context)
        val holder = adapter.onCreateViewHolder(parent, 0)
        
        adapter.onBindViewHolder(holder, 0)
        
        // Verificações visuais ou de estado se necessário, mas aqui validamos que o fluxo ocorreu
        assertEquals(1, adapter.itemCount)
    }
}
