package br.com.fenix.bilingualreader.view.components.book

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import br.com.fenix.bilingualreader.model.interfaces.PageCurl
import kotlin.math.abs
import kotlin.math.min


class StackPageTransform : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        val pageWidth = page.width.toFloat()
        if (position >= -1.0f && position <= 1.0f) {
            var v: Float
            if (position <= 0.0) {
                page.alpha = 1.0f + position
                page.translationX = pageWidth * -position
                v = 0.75f + 0.25f * (1.0f - abs(position))
                page.scaleX = v
                page.scaleY = v
            } else if (position > 0.5 && position <= 1.0) {
                page.alpha = 0.0f
                page.translationX = pageWidth * -position.toFloat()
            } else if (position > 0.3 && position <= 0.5) {
                page.alpha = 1.0f
                page.translationX = pageWidth * position.toFloat()
                v = 0.75f
                page.scaleX = v
                page.scaleY = v
            } else {
                if (position <= 0.3) {
                    page.alpha = 1.0f
                    page.translationX = pageWidth * position.toFloat()
                    v = (0.3 - position).toFloat()
                    v = min(v.toDouble(), 0.25).toFloat()
                    val scaleFactor = 0.75f + v
                    page.scaleX = scaleFactor
                    page.scaleY = scaleFactor
                }
            }
        } else
            page.alpha = 0.0f
    }
}

class ZoomPageTransform : ViewPager2.PageTransformer {
    private val MIN_SCALE: Float = 0.90f

    override fun transformPage(page: View, position: Float) {
        var alpha = 0f
        if (0 <= position && position <= 1) {
            alpha = 1 - position
        } else if (-1 < position && position < 0) {
            val scaleFactor = MIN_SCALE.coerceAtLeast(1 - abs(position))
            page.scaleX = scaleFactor
            page.scaleY = scaleFactor
            alpha = position + 1
        }

        page.alpha = alpha
    }
}

class CurlPageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        if (page is PageCurl) {
            // hold the page steady and let the views do the work
            if (position > -1.0f && position < 1.0f)
                page.translationX = -position * page.width
            else
                page.translationX = 0.0f

            if (position <= 1.0f && position >= -1.0f)
                (page as PageCurl).setCurlFactor(position)
        }
    }
}

class FadePageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        page.translationX = -position*page.width
        page.alpha = 1- abs(position)
    }
}

class DepthPageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        if (position < -1)
            page.alpha = 0f
        else if (position <= 0) {
            page.alpha = 1f
            page.translationX = 0f
            page.scaleX = 1f
            page.scaleY = 1f
        } else if (position <= 1) {
            page.translationX = -position*page.width
            page.alpha = 1- abs(position)
            page.scaleX = 1- abs(position)
            page.scaleY = 1- abs(position)
        } else
            page.alpha = 0f
    }
}