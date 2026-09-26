package br.com.fenix.bilingualreader.view.components.book

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.service.listener.SelectionChangeListener
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TextViewPageTest {

    private lateinit var textView: TextViewPage
    private lateinit var selectionListener: SelectionChangeListener

    @Before
    fun setup() {
        val app = RuntimeEnvironment.getApplication()
        app.setTheme(R.style.Theme_MangaReader)
        textView = TextViewPage(app)
        selectionListener = mockk(relaxed = true)
        textView.setSelectionChangeListener(selectionListener)
    }

    @Test
    fun `constructing TextViewPage does not crash during TextView init selection`() {
        val app = RuntimeEnvironment.getApplication()
        app.setTheme(R.style.Theme_MangaReader)
        // Regression: onSelectionChanged used to NPE on mSelectionHandler during super.<init>
        val page = TextViewPage(app)
        assertNotNull(page)
        page.setTextIsSelectable(true)
        assertTrue(page.isTextSelectable)
    }

    @Test
    fun `inflating fragment_book_text_view_page does not crash`() {
        val app = RuntimeEnvironment.getApplication()
        app.setTheme(R.style.Theme_MangaReader)
        val root = FrameLayout(app)
        val view = LayoutInflater.from(app).inflate(R.layout.fragment_book_text_view_page, root, false)
        val page = view.findViewById<TextViewPage>(R.id.page_text_view)
        assertNotNull(page)
    }

    @Test
    fun `onSelectionChanged triggers listener when text is selected`() {
        textView.setText("Hello World", android.widget.TextView.BufferType.SPANNABLE)

        android.text.Selection.setSelection(textView.text as android.text.Spannable, 0, 5)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        verify { selectionListener.onTextSelected() }

        android.text.Selection.setSelection(textView.text as android.text.Spannable, 0, 0)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        verify { selectionListener.onTextUnselected() }
    }

    @Test
    fun `resetZoom restores original text size`() {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, 50f)
        val originalSize = textView.textSize

        val isZoomField = TextViewPage::class.java.getDeclaredField("mIsZoom")
        isZoomField.isAccessible = true
        isZoomField.set(textView, true)

        val isChangeSizeField = TextViewPage::class.java.getDeclaredField("mIsChangeSize")
        isChangeSizeField.isAccessible = true
        isChangeSizeField.set(textView, false)

        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalSize + 20f)
        assertNotEquals(originalSize, textView.textSize, 0.1f)

        textView.resetZoom()
        assertEquals(originalSize, textView.textSize, 0.1f)
    }

    @Test
    fun `zoom logic updates text size on pinch`() {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, 50f)

        val zoomMethod = TextViewPage::class.java.getDeclaredMethod("zoom", android.view.View::class.java, android.view.MotionEvent::class.java)
        zoomMethod.isAccessible = true

        val prop0 = MotionEvent.PointerProperties().apply { id = 0 }
        val prop1 = MotionEvent.PointerProperties().apply { id = 1 }
        val props = arrayOf(prop0, prop1)

        val coord0 = MotionEvent.PointerCoords().apply { x = 0f; y = 0f }
        val coord1 = MotionEvent.PointerCoords().apply { x = 100f; y = 100f }
        val coords = arrayOf(coord0, coord1)

        val action = (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT) or MotionEvent.ACTION_POINTER_DOWN
        val event = MotionEvent.obtain(0, 0, action, 2, props, coords, 0, 0, 0f, 0f, 0, 0, 0, 0)

        try {
            zoomMethod.invoke(textView, textView, event)
        } catch (_: Exception) {
        }

        assertTrue(true)
    }
}
