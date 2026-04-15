package br.com.fenix.bilingualreader.view.components

import android.content.Context
import br.com.fenix.bilingualreader.R
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TriStateCheckBoxTest {

    private lateinit var context: Context
    private lateinit var checkBox: TriStateCheckBox

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.Theme_MangaReader)
        checkBox = TriStateCheckBox(context)
    }

    @Test
    fun `test initial state should be unchecked`() {
        assertEquals(TriStateCheckBox.STATE_UNCHECKED, checkBox.state)
        assertFalse(checkBox.isChecked)
    }

    @Test
    fun `test setting state updates isChecked`() {
        checkBox.state = TriStateCheckBox.STATE_INDETERMINATE
        assertTrue(checkBox.isChecked)
        
        checkBox.state = TriStateCheckBox.STATE_CHECKED
        assertTrue(checkBox.isChecked)
        
        checkBox.state = TriStateCheckBox.STATE_UNCHECKED
        assertFalse(checkBox.isChecked)
    }

    @Test
    fun `test state change listener triggers`() {
        var capturedState = -1
        checkBox.onStateChanged = { _, state ->
            capturedState = state
        }
        
        checkBox.state = TriStateCheckBox.STATE_CHECKED
        assertEquals(TriStateCheckBox.STATE_CHECKED, capturedState)
    }

    @Test
    fun `test clicking checkbox cycles states`() {
        // Initial: UNCHECKED
        checkBox.performClick()
        assertEquals(TriStateCheckBox.STATE_INDETERMINATE, checkBox.state)
        
        checkBox.performClick()
        assertEquals(TriStateCheckBox.STATE_CHECKED, checkBox.state)
        
        checkBox.performClick()
        assertEquals(TriStateCheckBox.STATE_UNCHECKED, checkBox.state)
    }
}
