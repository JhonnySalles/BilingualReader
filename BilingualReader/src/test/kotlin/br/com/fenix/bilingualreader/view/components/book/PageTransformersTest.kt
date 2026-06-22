package br.com.fenix.bilingualreader.view.components.book

import android.view.View
import br.com.fenix.bilingualreader.model.interfaces.PageCurl
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PageTransformersTest {

    @Test
    fun `DefaultPageTransformer hides page when out of range`() {
        val transformer = DefaultPageTransformer()
        val page = mockk<View>(relaxed = true)

        transformer.transformPage(page, -1.1f)
        verify { page.alpha = 0f }

        transformer.transformPage(page, 1.1f)
        verify { page.alpha = 0f }
    }

    @Test
    fun `DefaultPageTransformer resets page properties when in range`() {
        val transformer = DefaultPageTransformer()
        val page = mockk<View>(relaxed = true)

        transformer.transformPage(page, 0f)
        verify { page.alpha = 1f }
        verify { page.scaleX = 1f }
        verify { page.scaleY = 1f }
        verify { page.translationX = 0f }
        verify { page.translationY = 0f }
    }

    @Test
    fun `ZoomPageTransform calculates alpha and scale correctly`() {
        val transformer = ZoomPageTransform()
        val page = mockk<View>(relaxed = true)

        // Case: position in [0, 1]
        transformer.transformPage(page, 0.5f)
        verify { page.alpha = 0.5f }

        // Case: position in (-1, 0)
        transformer.transformPage(page, -0.5f)
        verify { page.alpha = 0.5f }
        verify { page.scaleX = 0.9f } // MIN_SCALE is 0.90f
    }

    @Test
    fun `FadePageTransformer calculates alpha correctly for horizontal`() {
        val transformer = FadePageTransformer(isVertical = false)
        val page = mockk<View>(relaxed = true)
        every { page.width } returns 1000

        transformer.transformPage(page, 0.5f)
        verify { page.alpha = 0.5f }
        verify { page.translationX = -500f }
        verify { page.translationY = 0f }
    }

    @Test
    fun `CurlPageTransformer delegates to PageCurl when applicable`() {
        val page = mockk<View>(relaxed = true, moreInterfaces = arrayOf(PageCurl::class))
        val transformer = CurlPageTransformer()

        every { page.width } returns 1000
        
        transformer.transformPage(page, 0.5f)
        
        verify { page.translationX = -500f }
        verify { (page as PageCurl).setCurlFactor(0.5f) }
    }

    @Test
    fun `Curl3DPageTransformer delegates to PageCurl when applicable`() {
        val page = mockk<View>(relaxed = true, moreInterfaces = arrayOf(PageCurl::class))
        val transformer = Curl3DPageTransformer()

        every { page.width } returns 1000
        
        transformer.transformPage(page, 0.5f)
        
        verify { page.translationX = -500f }
        verify { (page as PageCurl).setCurlFactor(0.5f) }
    }
}
