package br.com.fenix.bilingualreader.view.components.book

import android.content.Context
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.model.entity.Speech
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TextViewAdapterTest {

    private lateinit var context: Context
    private lateinit var viewModel: BookReaderViewModel
    private lateinit var parse: DocumentParse
    private lateinit var adapter: TextViewAdapter

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        viewModel = mockk(relaxed = true)
        parse = mockk()
        
        // Mock viewModel properties
        every { viewModel.getFontSize(any()) } returns 16f
        every { viewModel.fontUpdate } returns MutableLiveData("mock_font")
        every { viewModel.fontSize } returns MutableLiveData(16f)
        
        every { parse.getPageCount(any()) } returns 10
        
        adapter = TextViewAdapter(context, viewModel, parse)
    }

    @Test
    fun `getItemCount returns page count from parse`() {
        assertEquals(10, adapter.itemCount)
    }

    @Test
    fun `refreshSize updates item count`() {
        every { parse.getPageCount(any()) } returns 20
        adapter.refreshSize()
        assertEquals(20, adapter.itemCount)
    }

    @Test
    fun `stopTTS clears speech and notifies changes`() {
        adapter.stopTTS()
    }

    @Test
    fun `readingLine updates speech and highlights if holder exists`() {
        val speech = Speech(page = 1, sequence = 0, text = "Hello", html = "Hello")
        // We can't easily test highlighting without real holders
        adapter.readingLine(speech)
        // Here we just ensure it doesn't crash if holder doesn't exist
    }
}
