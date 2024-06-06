package br.com.fenix.bilingualreader.view.components.book

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.sqrt


open class TextViewPage(context: Context, attributeSet: AttributeSet?) : AppCompatTextView(context, attributeSet) {

    companion object {
        const val TEXT_MAX_SIZE = 140f
        const val TEXT_MIN_SIZE = 40f
        const val STEP = 4
    }

    constructor(context: Context) : this(context, null) {}

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }


    private lateinit var mGestureDetector: GestureDetector
    private var mOuterTouchListener: OnTouchListener? = null
    private var mIsChangeSize: Boolean = true
    private var mOriginalSize: Float
    private var mZooming = false
    var isOnlyImage = false

    init {
        mOriginalSize = textSize

        mGestureDetector = GestureDetector(context, MyTouchListener())
        super.setOnTouchListener { v, event ->
            v.performClick()

            if (event.pointerCount > 1) {
                setTextIsSelectable(false)
                zoom(v, event)
                parent.requestDisallowInterceptTouchEvent(true)
            } else
                setTextIsSelectable(true)

            if (mZooming)
                parent.requestDisallowInterceptTouchEvent(true)

            mGestureDetector.onTouchEvent(event)
            mOuterTouchListener?.onTouch(v, event)
            onTouchEvent(event)
            false
        }
    }

    override fun setTextSize(size: Float) {
        super.setTextSize(size)
        if (mIsChangeSize)
            mOriginalSize = textSize
    }

    override fun setTextSize(unit: Int, size: Float) {
        super.setTextSize(unit, size)
        if (mIsChangeSize)
            mOriginalSize = textSize
    }

    override fun setOnTouchListener(l: OnTouchListener?) {
        mOuterTouchListener = l
    }

    private var mBaseDistZoomIn = 0
    private var mBaseDistZoomOut = 0

    fun resetZoom() {
        if (mZooming)
            try {
                mIsChangeSize = false
                setTextSize(TypedValue.COMPLEX_UNIT_PX, mOriginalSize)
            } finally {
                mIsChangeSize = true
            }
    }

    private fun zoom(v: View?, event: MotionEvent): Boolean {
        if (event.pointerCount == 2) {
            try {
                mIsChangeSize = false

                val action = event.action
                val pure = action and MotionEvent.ACTION_MASK

                if (pure == MotionEvent.ACTION_POINTER_DOWN && textSize <= TEXT_MAX_SIZE && textSize >= TEXT_MIN_SIZE) {
                    mBaseDistZoomIn = getDistance(event)
                    mBaseDistZoomOut = getDistance(event)
                } else {
                    val currentDistance = getDistance(event)
                    if (currentDistance > mBaseDistZoomIn) {
                        var finalSize: Float = textSize + STEP
                        if (finalSize > TEXT_MAX_SIZE) {
                            finalSize = TEXT_MAX_SIZE
                        }
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, finalSize)
                        mZooming = true
                    } else {
                        if (currentDistance < mBaseDistZoomOut) {
                            var finalSize: Float = textSize - STEP
                            if (finalSize < TEXT_MIN_SIZE)
                                finalSize = TEXT_MIN_SIZE

                            setTextSize(TypedValue.COMPLEX_UNIT_PX, finalSize)
                            mZooming = true
                        }
                    }
                }
            } finally {
                mIsChangeSize = true
            }
            return true
        }
        return false
    }

    private fun getDistance(event: MotionEvent): Int {
        val dx = (event.getX(0) - event.getX(1)).toInt()
        val dy = (event.getY(0) - event.getY(1)).toInt()
        return sqrt((dx * dx + dy * dy).toDouble()).toInt()
    }

    inner class MyTouchListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            resetZoom()
            return true
        }
    }


}