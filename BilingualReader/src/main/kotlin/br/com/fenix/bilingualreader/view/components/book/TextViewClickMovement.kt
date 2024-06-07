package br.com.fenix.bilingualreader.view.components.book

import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.TextView
import kotlin.math.abs


class TextViewClickMovement : LinkMovementMethod() {

    private var pressedCoordinate: FloatArray? = null

    override fun initialize(widget: TextView?, text: Spannable?) {
    }

    override fun onKeyDown(widget: TextView?, text: Spannable?, keyCode: Int, event: KeyEvent?): Boolean {
        return false
    }

    override fun onKeyUp(widget: TextView?, text: Spannable?, keyCode: Int, event: KeyEvent?): Boolean {
        return false
    }

    override fun onKeyOther(view: TextView?, text: Spannable?, event: KeyEvent?): Boolean {
        return false
    }

    override fun onTakeFocus(widget: TextView?, text: Spannable?, direction: Int) {
    }

    override fun onTrackballEvent(widget: TextView?, text: Spannable?, event: MotionEvent?): Boolean {
        return false
    }

    override fun onTouchEvent(widget: TextView, text: Spannable, event: MotionEvent): Boolean {
        val action = event.action
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
            var x = event.x.toInt()
            var y = event.y.toInt()


            x -= widget.totalPaddingLeft
            y -= widget.totalPaddingTop

            val layout = widget.layout

            val off: Int = layout.getOffsetForHorizontal(layout.getLineForVertical(y), x.toFloat())

            val links = text.getSpans(off, off, ClickableSpan::class.java)
            if (action == MotionEvent.ACTION_UP) {
                if (pressedCoordinate != null) {
                    if (abs((pressedCoordinate!![0] - event.x).toDouble()) < 10 && abs((pressedCoordinate!![1] - event.y).toDouble()) < 10) {
                        links[0].onClick(widget)
                        pressedCoordinate = null
                    } else
                        pressedCoordinate = null
                }
            } else if (links.isNotEmpty())
                pressedCoordinate = floatArrayOf(event.x, event.y)
        }
        return false
    }

    override fun onGenericMotionEvent(widget: TextView?, text: Spannable?, event: MotionEvent?): Boolean {
        return false
    }

    override fun canSelectArbitrarily(): Boolean {
        return false
    }
}