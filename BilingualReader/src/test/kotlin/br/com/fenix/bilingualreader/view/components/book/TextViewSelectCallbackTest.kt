package br.com.fenix.bilingualreader.view.components.book

import android.content.ClipboardManager
import android.content.Context
import android.text.Selection
import android.text.SpannableString
import android.view.ActionMode
import android.view.MenuItem
import android.widget.TextView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.service.listener.TextSelectCallbackListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TextViewSelectCallbackTest {

    private lateinit var context: Context
    private lateinit var holder: TextViewAdapter.TextViewPagerHolder
    private lateinit var textView: TextViewPage
    private lateinit var listener: TextSelectCallbackListener
    private lateinit var callback: TextViewSelectCallback
    private lateinit var actionMode: ActionMode
    private lateinit var menuItem: MenuItem
    private lateinit var popupWindow: android.widget.PopupWindow

    @Before
    fun setup() {
        val app = RuntimeEnvironment.getApplication()
        app.setTheme(R.style.Theme_MangaReader)
        context = app
        holder = mockk(relaxed = true)
        
        popupWindow = mockk(relaxed = true)
        val contentView = mockk<android.view.View>(relaxed = true)
        val materialButton = mockk<com.google.android.material.button.MaterialButton>(relaxed = true)
        
        every { holder.popupTextSelect } returns popupWindow
        every { popupWindow.contentView } returns contentView
        every { contentView.findViewById<android.view.View>(any()) } returns materialButton
        
        textView = TextViewPage(context)
        textView.setText("Hello World", TextView.BufferType.SPANNABLE)
        every { holder.textView } returns textView
        
        listener = mockk(relaxed = true)
        actionMode = mockk(relaxed = true)
        menuItem = mockk(relaxed = true)
        
        callback = TextViewSelectCallback(context, holder, 0, { _, _, _ -> }, listener)
    }

    @Test
    fun `onActionItemClicked copy copies text and finishes action mode`() {
        Selection.setSelection(textView.text as SpannableString, 0, 5)
        every { menuItem.itemId } returns android.R.id.copy
        
        callback.onActionItemClicked(actionMode, menuItem)
        
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals("Hello", clipboard.primaryClip?.getItemAt(0)?.text.toString())
        
        val latestToast = ShadowToast.getLatestToast()
        assertNotNull(latestToast)
        
        verify { actionMode.finish() }
    }

    @Test
    fun `onActionItemClicked green mark calls listener and finishes action mode`() {
        Selection.setSelection(textView.text as android.text.Spannable, 0, 5)
        every { menuItem.itemId } returns R.id.menu_text_select_colors_green
        
        val annotation = BookAnnotation(
            id_book = 1L,
            page = 0,
            pages = 10,
            fontSize = 16f,
            type = br.com.fenix.bilingualreader.model.enums.MarkType.BookMark,
            chapterNumber = 1f,
            chapter = "Chapter 1",
            text = "Hello",
            range = intArrayOf(0, 5),
            annotation = "",
            color = Color.Green
        )
        every { listener.textSelectAddMark(any(), any(), any(), any(), any()) } returns annotation
        
        callback.onActionItemClicked(actionMode, menuItem)
        
        verify { listener.textSelectAddMark(0, "Hello", Color.Green, 0, 5) }
        verify { actionMode.finish() }
    }

    @Test
    fun `onActionItemClicked tts calls listener and finishes action mode`() {
        Selection.setSelection(textView.text as android.text.Spannable, 0, 5)
        every { menuItem.itemId } returns R.id.menu_text_select_functions_tts
        
        callback.onActionItemClicked(actionMode, menuItem)
        
        // Note: Code reads until end of line, so "Hello World" instead of "Hello"
        verify { listener.textSelectReadingFrom(0, any()) }
        verify { actionMode.finish() }
    }

    @Test
    fun `onDestroyActionMode dismisses popup when no selection`() {
        callback.onDestroyActionMode(actionMode)
        verify { popupWindow.dismiss() }
    }

    @Test
    fun `onDestroyActionMode keeps popup when selection is active`() {
        Selection.setSelection(textView.text as SpannableString, 0, 5)
        callback.onDestroyActionMode(actionMode)
        verify(exactly = 0) { popupWindow.dismiss() }
    }
}
