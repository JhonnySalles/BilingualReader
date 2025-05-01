package br.com.fenix.bilingualreader.view.components.book

import android.os.Handler
import android.text.Selection
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.method.MovementMethod
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.widget.TextView
import br.com.fenix.bilingualreader.model.interfaces.LongClickableSpan
import br.com.fenix.bilingualreader.service.listener.SelectionChangeListener
import org.slf4j.LoggerFactory
import kotlin.math.abs


class TextViewClickMovement : LinkMovementMethod() {

    companion object {
        private var sInstance: TextViewClickMovement? = null

        private const val LONG_CLICK_TIME = 1000L
        private var mSelectionListener: SelectionChangeListener? = null

        fun getInstance(selectionListener: SelectionChangeListener?): MovementMethod {
            if (sInstance == null)
                sInstance = TextViewClickMovement()
            mSelectionListener = selectionListener
            return sInstance!!
        }
    }

    private val mLOGGER = LoggerFactory.getLogger(TextViewClickMovement::class.java)

    private var mLongClickHandler: Handler = Handler()
    private var mIsLongPressed = false
    private var mPressedCoordinate: FloatArray? = null

    override fun onTouchEvent(widget: TextView?, buffer: Spannable?, event: MotionEvent?): Boolean {
        val action = event?.action ?: return false

        if (mPressedCoordinate != null && abs(mPressedCoordinate!![0] - event.x) >= 10 && abs(mPressedCoordinate!![1] - event.y) >= 10)
            mLongClickHandler.removeCallbacksAndMessages(null)

        if (event.pointerCount > 1) {
            mLongClickHandler.removeCallbacksAndMessages(null)
            return false
        }

        if (mSelectionListener != null && mSelectionListener!!.isShowingPopup())
            return false

        var consume = false
        if (action == MotionEvent.ACTION_CANCEL)
            mLongClickHandler.removeCallbacksAndMessages(null)
        else if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
            var x = event.x.toInt()
            var y = event.y.toInt()

            x -= widget!!.totalPaddingLeft
            y -= widget.totalPaddingTop

            x += widget.scrollX
            y += widget.scrollY

            val layout = widget.layout

            val line: Int = layout.getLineForVertical(y)
            val off: Int = layout.getOffsetForHorizontal(line, x.toFloat())

            val linksClick = buffer!!.getSpans(off, off, ClickableSpan::class.java)
            val linksLongClick = buffer.getSpans(off, off, LongClickableSpan::class.java)

            if (action == MotionEvent.ACTION_UP) {
                mLongClickHandler.removeCallbacksAndMessages(null)

                if (mPressedCoordinate != null) {
                    if (abs(mPressedCoordinate!![0] - event.x) < 10 && abs(mPressedCoordinate!![1] - event.y) < 10) {
                        if (linksLongClick != null && linksLongClick.isNotEmpty()) {
                            if (!mIsLongPressed) {
                                Selection.removeSelection(buffer)
                                linksLongClick[0].onClick(widget)
                                consume = true
                            }
                        } else if (linksClick != null && linksClick.isNotEmpty()) {
                            Selection.removeSelection(buffer)
                            linksClick[0].onClick(widget)
                            consume = true
                        }
                    }

                    mPressedCoordinate = null
                }

                mIsLongPressed = false
            } else {
                mPressedCoordinate = floatArrayOf(event.x, event.y)

                if (linksLongClick != null && linksLongClick.isNotEmpty()) {
                    mLongClickHandler.postDelayed({
                        Selection.setSelection(buffer, buffer.getSpanStart(linksLongClick[0]), buffer.getSpanEnd(linksLongClick[0]))
                        linksLongClick[0].onLongClick(widget)
                        mIsLongPressed = true
                    }, LONG_CLICK_TIME)
                }
            }
        }

        return consume
    }
}