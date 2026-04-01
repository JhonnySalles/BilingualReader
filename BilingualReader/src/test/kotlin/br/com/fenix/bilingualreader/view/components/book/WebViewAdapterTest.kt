package br.com.fenix.bilingualreader.view.components.book

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderViewModel
import br.com.fenix.bilingualreader.service.controller.WebInterface
import br.com.fenix.bilingualreader.service.japanese.Formatter
import br.com.fenix.bilingualreader.view.ui.popup.PopupKanji
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class WebViewAdapterTest {

    private lateinit var context: Context
    private lateinit var activity: Activity
    private lateinit var viewModel: BookReaderViewModel
    private lateinit var parse: DocumentParse
    private lateinit var adapter: WebViewAdapter

    @Before
    fun setup() {
        mockkObject(Formatter)
        every { Formatter.initializeAsync(any()) } returns Unit
        
        mockkConstructor(WebInterface::class)
        mockkConstructor(PopupKanji::class)
        
        context = RuntimeEnvironment.getApplication()
        activity = mockk(relaxed = true)
        viewModel = mockk(relaxed = true)
        parse = mockk()
        
        every { viewModel.getFontSize(any()) } returns 16f
        every { parse.getPageCount(any()) } returns 10
        
        adapter = WebViewAdapter(activity, context, viewModel, parse)
    }

    @Test
    fun `getItemCount returns page count from parse`() {
        assertEquals(10, adapter.itemCount)
    }

    @Test
    fun `changePages updates item count`() {
        every { parse.getPageCount(any()) } returns 20
        adapter.changePages()
        assertEquals(20, adapter.itemCount)
    }

    @Test
    fun `onBindViewHolder calls prepareHtml`() {
        val holder = mockk<WebViewAdapter.WebViewPagerHolder>(relaxed = true)
        val webView = mockk<WebViewPage>(relaxed = true)
        every { holder.webViewPage } returns webView
        
        adapter.onBindViewHolder(holder, 5)
        
        verify { viewModel.prepareHtml(parse, 5, webView, any(), any()) }
    }
}
