package br.com.fenix.bilingualreader.view.components

import android.provider.Settings
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ComponentsUtilTest {

    @Test
    fun `canDrawOverlays returns value from Settings`() {
        val context = RuntimeEnvironment.getApplication()
        mockkStatic(Settings::class)
        
        every { Settings.canDrawOverlays(any()) } returns true
        assertTrue(ComponentsUtil.canDrawOverlays(context))
        
        every { Settings.canDrawOverlays(any()) } returns false
        assertFalse(ComponentsUtil.canDrawOverlays(context))
    }
}
