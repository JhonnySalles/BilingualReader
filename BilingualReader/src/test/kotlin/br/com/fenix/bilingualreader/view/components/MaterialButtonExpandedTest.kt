package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.util.AttributeSet
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MaterialButtonExpandedTest {

    private lateinit var context: Context
    private lateinit var attrs: AttributeSet
    private lateinit var button: MaterialButtonExpanded

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        context.setTheme(br.com.fenix.bilingualreader.R.style.Theme_MangaReader)
        attrs = Robolectric.buildAttributeSet().build()
        button = MaterialButtonExpanded(context, attrs)
    }

    @Test
    fun `setIsExpanded updates drawable state`() {
        button.setIsExpanded(true)
        button.refreshDrawableState()
        val state = button.drawableState
        assertTrue(state!!.any { it == br.com.fenix.bilingualreader.R.attr.state_expanded })
    }
}
