package br.com.fenix.bilingualreader.view.components

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DottedSeekBarTest {

    private lateinit var context: Context
    private lateinit var seekBar: DottedSeekBar

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        context.setTheme(br.com.fenix.bilingualreader.R.style.Theme_MangaReader)
        seekBar = DottedSeekBar(context)
    }

    @Test
    fun `setDots does not crash`() {
        val dots = intArrayOf(1, 2, 3)
        seekBar.setDots(dots, dots, dots, dots)
    }

    @Test
    fun `setDotsMode does not crash`() {
        seekBar.setDotsMode(true)
        seekBar.setDotsMode(false)
    }

    @Test
    fun `setDotsPrimaryDrawable does not crash`() {
        seekBar.setDotsPrimaryDrawable(android.R.drawable.ic_menu_edit)
    }
}
