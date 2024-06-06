package br.com.fenix.bilingualreader.view.components.manga

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.LinearLayout
import androidx.core.view.GestureDetectorCompat
import br.com.fenix.bilingualreader.service.listener.WindowListener


class ResizeView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayout(context, attributeSet, defStyleAttr), GestureDetector.OnGestureListener {

    var mWindowListener: WindowListener? = null
    var mDetector: GestureDetectorCompat? = null

    init {
        init(context)
    }

    private fun init(context: Context) {
        mDetector = GestureDetectorCompat(context, this)
    }

    fun setWindowListener(windowListener: WindowListener) {
        this.mWindowListener = windowListener
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        performClick()
        mDetector?.onTouchEvent(e)
        return mWindowListener?.onResize(e) ?: false
    }

    override fun onDown(e: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(p0: MotionEvent?, e1: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {}

    override fun onFling(p0: MotionEvent?, e1: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

}