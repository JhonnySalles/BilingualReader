package br.com.fenix.bilingualreader.view.adapter.book

import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookSearch
import br.com.fenix.bilingualreader.service.listener.BookSearchHistoryListener
import com.google.android.material.button.MaterialButton
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BookSearchHistoryAdapterTest {

    private lateinit var context: Context
    private lateinit var adapter: BookSearchHistoryAdapter
    private lateinit var mockListener: BookSearchHistoryListener
    private val dataSet = ArrayList<BookSearch>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.Theme_MangaReader)
        mockListener = mockk(relaxed = true)
        dataSet.clear()
        adapter = BookSearchHistoryAdapter(context, R.layout.line_card_book_search_history, dataSet, mockListener)
    }

    @Test
    fun `getView should populate title and date correctly`() {
        val search = BookSearch(1L, 10L, "Query", LocalDateTime.now().minusDays(2))
        dataSet.add(search)

        val view = adapter.getView(0, null, LinearLayout(context))

        val title = view.findViewById<TextView>(R.id.book_search_history_title)
        val date = view.findViewById<TextView>(R.id.book_search_date)

        assertEquals("Query", title.text.toString())
        // Since it's 2 days ago, formatCountDays should return something like "2 days ago" 
        // We can check if it's not empty and reflects the logic in GeneralConsts
        assert(date.text.isNotEmpty())
    }

    @Test
    fun `clicking root should trigger listener onClick`() {
        val search = BookSearch(1L, 10L, "Query", LocalDateTime.now())
        dataSet.add(search)

        val view = adapter.getView(0, null, LinearLayout(context))
        val root = view.findViewById<LinearLayout>(R.id.book_search_history)

        root.performClick()

        verify { mockListener.onClick(search) }
    }

    @Test
    fun `clicking delete should trigger listener onDelete`() {
        val search = BookSearch(1L, 10L, "Query", LocalDateTime.now())
        dataSet.add(search)

        val view = adapter.getView(0, null, LinearLayout(context))
        val deleteBtn = view.findViewById<MaterialButton>(R.id.book_search_history_delete)

        deleteBtn.performClick()

        verify { mockListener.onDelete(search, any(), 0) }
    }
}

