package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.RelativeLayout
import androidx.core.view.GestureDetectorCompat
import br.com.fenix.bilingualreader.service.listener.WindowListener

class WindowView @JvmOverloads constructor(context: Context, attrs:AttributeSet? = null, defStyleAttr: Int =0): RelativeLayout(context, attrs, defStyleAttr) {

    private var mWindowListener: WindowListener? = null
    private var mDetector: GestureDetectorCompat? = null

    fun setWindowListener(windowListener: WindowListener) {
        mWindowListener = windowListener
    }

    fun setDetector(detector: GestureDetectorCompat?) {
        mDetector = detector
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        performClick()
        mDetector?.onTouchEvent(e)
        return mWindowListener?.onTouch(e)?: false
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}