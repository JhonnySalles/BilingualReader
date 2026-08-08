package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.RelativeLayout
import br.com.fenix.bilingualreader.service.listener.WindowListener

class WindowView @JvmOverloads constructor(context: Context, attrs:AttributeSet? = null, defStyleAttr: Int =0): RelativeLayout(context, attrs, defStyleAttr) {

    private var mWindowListener: WindowListener? = null
    private var mDetector: GestureDetector? = null

    fun setWindowListener(windowListener: WindowListener) {
        mWindowListener = windowListener
    }

    fun setDetector(detector: GestureDetector?) {
        mDetector = detector
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        mDetector?.onTouchEvent(e)
        if (e.action == MotionEvent.ACTION_UP)
            performClick()
        return mWindowListener?.onTouch(e) ?: false
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
