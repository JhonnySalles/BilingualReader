package br.com.fenix.bilingualreader.view.components.manga

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.viewpager.widget.ViewPager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.PaginationType
import br.com.fenix.bilingualreader.model.enums.ScrollingType
import br.com.fenix.bilingualreader.model.interfaces.PageCurl
import org.slf4j.LoggerFactory
import kotlin.math.abs


class ImageViewPager(context: Context, attributeSet: AttributeSet) : ViewPager(context, attributeSet) {

    private val mLOGGER = LoggerFactory.getLogger(ZoomRecyclerView::class.java)

    private var mStartPos = 0f
    private var mSwipeOutListener: OnSwipeOutListener? = null
    private var mScrolling = ScrollingType.Pagination
    private var mPaginationType = PaginationType.Default
    private var mElevation = context.resources.getDimension(R.dimen.reader_elevation)

    interface OnSwipeOutListener {
        fun onSwipeOutAtStart()
        fun onSwipeOutAtEnd()
    }

    fun setOnSwipeOutListener(listener: OnSwipeOutListener?) {
        mSwipeOutListener = listener
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        performClick()
        if (ev!!.action == MotionEvent.ACTION_UP) {
            val diff = if (mScrolling == ScrollingType.Vertical) (ev.y - mStartPos) else (ev.x - mStartPos)
            if (diff > 0 && currentItem == 0)
                mSwipeOutListener?.onSwipeOutAtStart()
            else if (diff < 0 && currentItem == adapter!!.count - 1)
                mSwipeOutListener?.onSwipeOutAtEnd()
        }
        return super.onTouchEvent(if (mScrolling == ScrollingType.Vertical) swapXY(ev) else ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return if (mScrolling == ScrollingType.Vertical) {
            if (ev.action == MotionEvent.ACTION_DOWN)
                mStartPos = ev.y

            val event = super.onInterceptTouchEvent(swapXY(ev))
            swapXY(ev)
            return event
        } else {
            if (ev.action == MotionEvent.ACTION_DOWN)
                mStartPos = ev.x
            super.onInterceptTouchEvent(ev)
        }
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    fun setSwipeOrientation(scrolling: ScrollingType, pagination: PaginationType) {
        if (mScrolling != scrolling || mPaginationType != pagination) {
            mPaginationType = pagination
            mScrolling = scrolling
            initSwipeMethods()
        }
    }

    private fun initSwipeMethods() {
        when (mPaginationType) {
            PaginationType.Default -> {
                if (mScrolling == ScrollingType.Vertical)
                    setPageTransformer(false, VerticalPageTransformer())
                else
                    setPageTransformer(false, HorizontalPageTransformer())}
            PaginationType.Stack -> setPageTransformer(mScrolling != ScrollingType.HorizontalRightToLeft, StackPageTransform(mScrolling, mElevation))
            PaginationType.CurlPage -> setPageTransformer(false, CurlPageTransformer())
            PaginationType.Zooming -> setPageTransformer(false, ZoomPageTransform(mScrolling == ScrollingType.Vertical))
            PaginationType.Depth -> setPageTransformer(mScrolling != ScrollingType.HorizontalRightToLeft, DepthPageTransformer(mScrolling))
            PaginationType.Fade -> setPageTransformer(false, FadePageTransformer())
        }
        overScrollMode = if (mScrolling == ScrollingType.Vertical) OVER_SCROLL_NEVER else OVER_SCROLL_IF_CONTENT_SCROLLS
    }

    private fun swapXY(event: MotionEvent): MotionEvent {
        val width = width.toFloat()
        val height = height.toFloat()

        val newX = (event.y / height) * width
        val newY = (event.x / width) * height

        event.setLocation(newX, newY)
        return event
    }

    private class VerticalPageTransformer : PageTransformer {
        override fun transformPage(page: View, position: Float) {
            if (position < -1)
                page.alpha = 0f
            else if (position <= 1) {
                page.background = null
                page.translationZ = 0f
                page.elevation = 0f
                page.alpha = 1f
                page.scaleX = 1f
                page.scaleY = 1f
                page.translationX = page.width * -position
                page.translationY = position * page.height
            } else
                page.alpha = 0f
        }
    }

    private class HorizontalPageTransformer : PageTransformer {
        override fun transformPage(page: View, position: Float) {
            if (position < -1)
                page.alpha = 0f
            else if (position <= 1) {
                page.background = null
                page.translationZ = 0f
                page.elevation = 0f
                page.scaleX = 1f
                page.scaleY = 1f
                page.alpha = 1f
                page.translationX = 0f
                page.translationY = 0f
            } else
                page.alpha = 0f
        }
    }

    private class StackPageTransform(val scrolling: ScrollingType, val elevation : Float) : PageTransformer {
        override fun transformPage(page: View, position: Float) {
            if (position < -1)
                page.alpha = 0f
            else if (position <= 1) {
                page.setBackgroundResource(R.drawable.app_background)
                page.translationZ = 0f
                page.elevation = 0f
                page.scaleX = 1f
                page.scaleY = 1f
                page.alpha = 1f
                when (scrolling) {
                    ScrollingType.Vertical -> {
                        page.translationX = page.width * -position
                        page.translationY = if (position < 0) position * page.height else 0f

                        if (position < 0) {
                            page.translationZ = 20f
                            page.elevation = elevation
                        }
                    }
                    ScrollingType.HorizontalRightToLeft -> {
                        page.translationX = if (position == 0f) 0f else if (position > 0) position else position * -page.width
                        page.translationY = 0f

                        if (position >= 0) {
                            page.translationZ = 20f
                            page.elevation = elevation
                        }
                    }
                    else -> {
                        page.translationX = if (position < 0) 0f else position * -page.width
                        page.translationY = 0f

                        if (position < 0) {
                            page.translationZ = 20f
                            page.elevation = elevation
                        }
                    }
                }
            } else
                page.alpha = 0f
        }
    }

    private class ZoomPageTransform(var isVertical: Boolean) : PageTransformer {
        private val MIN_SCALE: Float = 0.90f

        override fun transformPage(page: View, position: Float) {
            page.background = null
            page.translationZ = 0f
            page.elevation = 0f
            page.scaleX = 1f
            page.scaleY = 1f
            page.translationY = 0f
            page.translationX = 0f

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
            if (isVertical) {
                page.translationX = page.width * -position
                page.translationY = position * page.height
            }
        }
    }

    private class CurlPageTransformer : PageTransformer {
        override fun transformPage(page: View, position: Float) {
            if (position < -1)
                page.alpha = 0f
            else if (position <= 1) {
                page.background = null
                page.translationZ = 0f
                page.elevation = 0f
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

    private class FadePageTransformer : PageTransformer {
        override fun transformPage(page: View, position: Float) {
            if (position < -1)
                page.alpha = 0f
            else if (position <= 1) {
                page.background = null
                page.translationZ = 0f
                page.elevation = 0f
                page.scaleX = 1f
                page.scaleY = 1f
                page.translationY = 0f
                page.translationX = -position*page.width
                page.alpha = 1- abs(position)
            } else
                page.alpha = 0f
        }
    }

    private class DepthPageTransformer(var scrolling: ScrollingType) : PageTransformer {
        override fun transformPage(page: View, position: Float) {
            if (position < -1)
                page.alpha = 0f
            else if (position <= 1) {
                page.background = null
                page.translationZ = 0f
                page.elevation = 0f

                when (scrolling) {
                    ScrollingType.Vertical -> {
                        if (position <= 0) {
                            page.alpha = 1f
                            page.scaleX = 1f
                            page.scaleY = 1f
                            page.translationX = page.width * -position
                            page.translationY = if (position < 0) position * page.height else 0f
                        } else {
                            page.translationX = page.width * -position
                            page.translationY = 0f
                            page.alpha = 1- abs(position)
                            page.scaleX = 1- abs(position)
                            page.scaleY = 1- abs(position)
                        }
                    }
                    ScrollingType.HorizontalRightToLeft -> {
                        if (position <= 0) {
                            page.translationY = 0f
                            page.translationX = page.width * -position
                            page.alpha = 1- abs(position)
                            page.scaleX = 1- abs(position)
                            page.scaleY = 1- abs(position)
                        } else {
                            page.translationY = 0f
                            page.translationX = position
                            page.alpha = 1f
                            page.scaleX = 1f
                            page.scaleY = 1f
                        }
                    }
                    else -> {
                        if (position <= 0) {
                            page.alpha = 1f
                            page.translationY = 0f
                            page.translationX = 0f
                            page.scaleX = 1f
                            page.scaleY = 1f
                        } else {
                            page.translationY = 0f
                            page.translationX = -position * page.width
                            page.alpha = 1- abs(position)
                            page.scaleX = 1- abs(position)
                            page.scaleY = 1- abs(position)
                        }
                    }
                }
            } else
                page.alpha = 0f
        }
    }

}