package br.com.fenix.bilingualreader.view.components.book

import android.content.Context
import android.text.Spannable
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import br.com.fenix.bilingualreader.service.listener.SelectionChangeListener
import kotlin.math.abs
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
    private var mSelectionListener: SelectionChangeListener? = null
    private var mIsChangeSize: Boolean = true
    private var mOriginalSize: Float
    private var mZooming = false
    var isOnlyImage = false

    init {
        mOriginalSize = textSize

        mGestureDetector = GestureDetector(context, MyTouchListener())
        super.setOnTouchListener { view, event ->
            view.performClick()

            if (hasClickSpan(event))
                return@setOnTouchListener false

            if (event.pointerCount > 1) {
                setTextIsSelectable(false)
                zoom(view, event)
                parent.requestDisallowInterceptTouchEvent(true)
            } else
                setTextIsSelectable(true)

            if (mZooming)
                parent.requestDisallowInterceptTouchEvent(true)

            mGestureDetector.onTouchEvent(event)
            mOuterTouchListener?.onTouch(view, event)
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

    override fun setOnTouchListener(listenner: OnTouchListener?) {
        mOuterTouchListener = listenner
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

    private var pressedCoordinate: FloatArray? = null
    private fun hasClickSpan(event: MotionEvent): Boolean {
        var consume = false
        if (text is Spannable && (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_UP)) {
            var x = event.x.toInt()
            var y = event.y.toInt()


            x -= totalPaddingLeft
            y -= totalPaddingTop

            val off: Int = layout.getOffsetForHorizontal(layout.getLineForVertical(y), x.toFloat())

            val links = (text as Spannable).getSpans(off, off, ClickableSpan::class.java)
            if (event.action == MotionEvent.ACTION_UP) {
                if (pressedCoordinate != null) {
                    if (abs((pressedCoordinate!![0] - event.x).toDouble()) < 10 && abs((pressedCoordinate!![1] - event.y).toDouble()) < 10)
                        consume = true
                    else
                        pressedCoordinate = null
                }
            } else if (links.isNotEmpty())
                pressedCoordinate = floatArrayOf(event.x, event.y)
        }
        return consume
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