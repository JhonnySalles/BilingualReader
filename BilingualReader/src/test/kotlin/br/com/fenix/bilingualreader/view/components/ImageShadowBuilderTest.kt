package br.com.fenix.bilingualreader.view.components

import android.graphics.Bitmap
import android.graphics.Point
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ImageShadowBuilderTest {

    @Test
    fun `onProvideShadowMetrics sets correct values`() {
        val bitmap = mockk<Bitmap>()
        every { bitmap.width } returns 100
        every { bitmap.height } returns 200
        
        val builder = ImageShadowBuilder(bitmap)
        val shadowSize = Point()
        val shadowTouchPoint = Point()
        
        builder.onProvideShadowMetrics(shadowSize, shadowTouchPoint)
        
        assertEquals(100, shadowSize.x)
        assertEquals(200, shadowSize.y)
        assertEquals(50, shadowTouchPoint.x)
        assertEquals(100, shadowTouchPoint.y)
    }
}
