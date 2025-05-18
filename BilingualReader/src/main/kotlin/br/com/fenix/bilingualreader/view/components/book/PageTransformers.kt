package br.com.fenix.bilingualreader.view.components.book

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import br.com.fenix.bilingualreader.model.interfaces.PageCurl
import kotlin.math.abs
import kotlin.math.min


class DefaultPageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        if (position < -1)
            page.alpha = 0f
        else if (position <= 1) {
            page.scaleX = 1f
            page.scaleY = 1f
            page.alpha = 1f
            page.translationX = 0f
            page.translationY = 0f
        } else
            page.alpha = 0f
    }
}

class StackPageTransform(val isVertical: Boolean) : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        if (position >= -1.0f && position <= 1.0f) {
            if (isVertical)
                page.translationX = 0f
            else
                page.translationY = 0f

            var v: Float
            if (position <= 0.0) {
                page.alpha = 1.0f + position
                v = 0.75f + 0.25f * (1.0f - abs(position))
                page.scaleX = v
                page.scaleY = v

                if (isVertical)
                    page.translationY = page.height * -position
                else
                    page.translationX = page.width * -position

            } else if (position > 0.5 && position <= 1.0) {
                page.alpha = 0.0f

                if (isVertical)
                    page.translationY = page.height * -position
                else
                    page.translationX = page.width * -position

            } else if (position > 0.3 && position <= 0.5) {
                page.alpha = 1.0f
                v = 0.75f
                page.scaleX = v
                page.scaleY = v

                if (isVertical)
                    page.translationY = page.height * position
                else
                    page.translationX = page.width * position

            } else {
                if (position <= 0.3) {
                    page.alpha = 1.0f
                    v = 0.3f - position
                    v = min(v, 0.25f)
                    val scaleFactor = 0.75f + v
                    page.scaleX = scaleFactor
                    page.scaleY = scaleFactor

                    if (isVertical)
                        page.translationY = page.height * position
                    else
                        page.translationX = page.width * position
                }
            }
        } else
            page.alpha = 0.0f
    }
}

class ZoomPageTransform : ViewPager2.PageTransformer {
    private val MIN_SCALE: Float = 0.90f

    override fun transformPage(page: View, position: Float) {
        page.translationX = 0f
        page.translationY = 0f

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
        if (position < -1)
            page.alpha = 0f
        else if (position <= 1) {
            page.scaleX = 1f
            page.scaleY = 1f
            page.alpha = 1f
            page.translationY = 0f

            if (page is PageCurl) {
                // hold the page steady and let the views do the work
                if (position > -1.0f && position < 1.0f)
                    page.translationX = -position * page.width
                else
                    page.translationX = 0.0f

                (page as PageCurl).setCurlFactor(position)
            } else
                page.translationX = 0f
        } else
            page.alpha = 0f
    }
}

class FadePageTransformer(val isVertical: Boolean)  : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        if (position < -1)
            page.alpha = 0f
        else if (position <= 1) {
            page.scaleX = 1f
            page.scaleY = 1f

            if (isVertical) {
                page.translationY = -position * page.height
                page.translationX = 0f
            } else {
                page.translationY = 0f
                page.translationX = -position * page.width
            }

            page.alpha = 1- abs(position)
        } else
            page.alpha = 0f
    }
}

class DepthPageTransformer(val isVertical: Boolean) : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        if (position < -1)
            page.alpha = 0f
        else if (position <= 1) {
            if (position <= 0) {
                page.alpha = 1f
                page.translationY = 0f
                page.translationX = 0f
                page.scaleX = 1f
                page.scaleY = 1f
            } else {
                page.alpha = 1- abs(position)
                page.scaleX = 1- abs(position)
                page.scaleY = 1- abs(position)

                if (isVertical) {
                    page.translationY = -position * page.height
                    page.translationX = 0f
                } else {
                    page.translationY = 0f
                    page.translationX = -position * page.width
                }
            }
        } else
            page.alpha = 0f
    }
}