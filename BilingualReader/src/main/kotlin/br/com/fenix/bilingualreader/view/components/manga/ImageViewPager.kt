package br.com.fenix.bilingualreader.view.components.manga

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.viewpager.widget.ViewPager


class ImageViewPager(context: Context, attributeSet: AttributeSet) : ViewPager(context, attributeSet) {
    private var mStartPos = 0f
    private var mSwipeOutListener: OnSwipeOutListener? = null
    private var mIsVertical = false

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
            val diff = if (mIsVertical) (ev.y - mStartPos) else (ev.x - mStartPos)
            if (diff > 0 && currentItem == 0)
                mSwipeOutListener?.onSwipeOutAtStart()
            else if (diff < 0 && currentItem == adapter!!.count - 1)
                mSwipeOutListener?.onSwipeOutAtEnd()
        }
        return super.onTouchEvent(if (mIsVertical) swapXY(ev) else ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return if (mIsVertical) {
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

    fun setSwipeOrientation(isVertical: Boolean) {
        if (mIsVertical != isVertical) {
            mIsVertical = isVertical
            initSwipeMethods()
        }
    }

    private fun initSwipeMethods() {
        if (mIsVertical) {
            setPageTransformer(false, VerticalPageTransformer())
            overScrollMode = OVER_SCROLL_NEVER
        } else {
            setPageTransformer(false, HorizontalPageTransformer())
            overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS
        }
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
                page.alpha = 1f
                page.translationX = page.width * -position
                val yPosition = position * page.height
                page.translationY = yPosition
            } else
                page.alpha = 0f
        }
    }

    private class HorizontalPageTransformer : PageTransformer {
        override fun transformPage(page: View, position: Float) {
            if (position < -1)
                page.alpha = 0f
            else if (position <= 1) {
                page.alpha = 1f
                page.translationX = 0f
                page.translationY = 0f
            } else
                page.alpha = 0f
        }
    }
}