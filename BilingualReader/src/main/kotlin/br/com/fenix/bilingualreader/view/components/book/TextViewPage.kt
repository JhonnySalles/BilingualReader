package br.com.fenix.bilingualreader.view.components.book

import android.content.Context
import android.text.Spannable
import android.text.method.MovementMethod
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import br.com.fenix.bilingualreader.service.listener.SelectionChangeListener
import org.slf4j.LoggerFactory
import kotlin.math.sqrt


open class TextViewPage(context: Context, attributeSet: AttributeSet?) : AppCompatTextView(context, attributeSet) {

    companion object {
        const val TEXT_MAX_SIZE = 140f
        const val TEXT_MIN_SIZE = 40f
        const val STEP = 4
    }

    constructor(context: Context) : this(context, null) {}

    private val mLOGGER = LoggerFactory.getLogger(TextViewPage::class.java)

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private var mCustomMovement: MovementMethod? = null
    private var mGestureDetector: GestureDetector
    private var mOuterTouchListener: OnTouchListener? = null
    private var mSelectionListener: SelectionChangeListener? = null
    private var mIsChangeSize: Boolean = true
    private var mOriginalSize: Float
    private var mIsZoom = false

    init {
        mOriginalSize = textSize

        mGestureDetector = GestureDetector(context, SimpleGestureListener())
        super.setOnTouchListener { view, event ->
            view.performClick()

            if (mCustomMovement != null) {
                if (text is Spannable && mCustomMovement!!.onTouchEvent(this, text as Spannable, event))
                    return@setOnTouchListener true
            }

            if (event.pointerCount > 1) {
                setTextIsSelectable(false)
                zoom(view, event)
                parent.requestDisallowInterceptTouchEvent(true)
                return@setOnTouchListener true
            } else
                setTextIsSelectable(true)

            if (mGestureDetector.onTouchEvent(event))
                return@setOnTouchListener true

            if (mOuterTouchListener != null && mOuterTouchListener!!.onTouch(view, event))
                return@setOnTouchListener true

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

    override fun setOnTouchListener(listener: OnTouchListener?) {
        mOuterTouchListener = listener
    }

    fun setCustomMovement(movement: MovementMethod) {
        mCustomMovement = movement
        movementMethod = movement
    }

    override fun getDefaultMovementMethod(): MovementMethod? {
        return mCustomMovement ?: super.getDefaultMovementMethod()
    }

    fun setSelectionChangeListener(listener: SelectionChangeListener) {
        mSelectionListener = listener
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (hasSelection())
            mSelectionListener?.onTextSelected()
        else
            mSelectionListener?.onTextUnselected()
    }

    private var mLastZoomDistance = 0f

    fun resetZoom() {
        if (mIsZoom)
            try {
                mIsChangeSize = false
                setTextSize(TypedValue.COMPLEX_UNIT_PX, mOriginalSize)
                mIsZoom = false
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
                    mLastZoomDistance = getDistance(event)
                } else {
                    var finalSize = textSize
                    val currentDistance = getDistance(event)
                    if (currentDistance > mLastZoomDistance) {
                        finalSize = textSize + STEP
                        if (finalSize > TEXT_MAX_SIZE)
                            finalSize = TEXT_MAX_SIZE
                    } else if (currentDistance < mLastZoomDistance) {
                        finalSize = textSize - STEP
                        if (finalSize < TEXT_MIN_SIZE)
                            finalSize = TEXT_MIN_SIZE
                    }

                    mLastZoomDistance = currentDistance
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, finalSize)
                    mIsZoom = finalSize != TEXT_MIN_SIZE
                }
            } finally {
                mIsChangeSize = true
            }
            return true
        }
        return false
    }

    private fun getDistance(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return sqrt(dx * dx + dy * dy)
    }

    inner class SimpleGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            resetZoom()
            return true
        }
    }
}