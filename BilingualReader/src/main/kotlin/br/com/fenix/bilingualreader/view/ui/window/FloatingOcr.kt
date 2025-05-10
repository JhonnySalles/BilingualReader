package br.com.fenix.bilingualreader.view.ui.window

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.annotation.Dimension
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.view.GestureDetectorCompat
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.service.listener.WindowListener
import br.com.fenix.bilingualreader.service.ocr.OcrProcess
import br.com.fenix.bilingualreader.service.ocr.Tesseract
import br.com.fenix.bilingualreader.view.components.WindowView
import br.com.fenix.bilingualreader.view.components.manga.ResizeView
import org.slf4j.LoggerFactory
import kotlin.math.abs


class FloatingOcr constructor(
    private val context: Context,
    private val activity: AppCompatActivity,
    private val parent : View
) : WindowListener, GestureDetector.OnDoubleTapListener {

    private val mLOGGER = LoggerFactory.getLogger(FloatingOcr::class.java)
    private var mPopup: PopupWindow? = null
        get() {
            if (field == null) {
                field = PopupWindow(parent, mLastSize.width, mLastSize.height)
                field?.contentView = mFloatingView
                field?.isFocusable = false
                field?.isOutsideTouchable = false
                field?.setOnDismissListener { isShowing = false }
                mPosition = Point(parent.height / 2 - mLastSize.height / 2, parent.width / 2 - mLastSize.width / 2)
            }
            return field
        }

    private var mFloatingView: View = LayoutInflater.from(context).inflate(R.layout.floating_window_ocr, null)

    private var mLastSize : Size
    private var mPosition : Point
    var isShowing = false
    private var mDX = 0
    private var mDY = 0
    private var minSize = 0

    init {
        mPosition = Point(parent.height / 2, parent.width / 2)
        mLastSize = Size(context.resources.getDimension(R.dimen.floating_ocr_width).toInt(), context.resources.getDimension(R.dimen.floating_ocr_height).toInt())
        minSize = context.resources.getDimension(R.dimen.floating_ocr_min_size).toInt()

        with(mFloatingView) {
            mCloseButton = this.findViewById(R.id.window_ocr_close)
            mCloseButton.setOnClickListener { dismiss() }

            val windowView: WindowView = this.findViewById(R.id.window_ocr_clickable)
            val resizeView: ResizeView = this.findViewById(R.id.window_ocr_resize)

            windowView.setWindowListener(this@FloatingOcr as WindowListener)
            resizeView.setWindowListener(this@FloatingOcr as WindowListener)

            val detectorCompat = GestureDetectorCompat(context, this@FloatingOcr as WindowListener)
            windowView.setDetector(detectorCompat)
        }
    }

    private lateinit var mCloseButton: AppCompatImageButton
    private val mDismissCloseButton = Runnable {
        if (mCloseButton.visibility == View.VISIBLE) {
            mCloseButton.animate().alpha(0.0f).setDuration(300L)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        mCloseButton.visibility = View.GONE
                        mFloatingView.layoutParams.width -= context.resources.getDimension(R.dimen.floating_ocr_button_close_size).toInt() + context.resources.getDimension(R.dimen.floating_ocr_button_close_margin).toInt()
                        mPopup?.update(mPosition.x, mPosition.y, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    }
                })
        }
    }
    private val mHandler = Handler(Looper.getMainLooper())

    private var mTouchParamUpdateTimer = System.currentTimeMillis()
    private var mSizeParamUpdateTimer = System.currentTimeMillis()

    override fun onScroll(p0: MotionEvent?, e1: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        mPosition.x = mDX + e1.rawX.toInt()
        mPosition.y = mDY + e1.rawY.toInt()
        fixBoxBounds()
        mPopup?.update(mPosition.x, mPosition.y, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        return false
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        return false
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN)
            processTesseractAsync()

        return false
    }

    private fun onUp(e: MotionEvent): Boolean {
        return false
    }

    override fun onDown(e: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mHandler.hasCallbacks(mDismissCloseButton))
                mHandler.removeCallbacks(mDismissCloseButton)
        } else
            mHandler.removeCallbacks(mDismissCloseButton)

        mHandler.postDelayed(mDismissCloseButton, 3000)

        if (mCloseButton.visibility != View.VISIBLE) {
            mCloseButton.visibility = View.VISIBLE
            mCloseButton.alpha = 0.0f
            mCloseButton.animate().alpha(1.0f).setDuration(300L)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        mCloseButton.visibility = View.VISIBLE
                    }
                })

            mFloatingView.layoutParams.width += context.resources.getDimension(R.dimen.floating_ocr_button_close_size).toInt() + context.resources.getDimension(R.dimen.floating_ocr_button_close_margin).toInt()
            mPopup?.update(mPosition.x, mPosition.y, mFloatingView.layoutParams.width, mFloatingView.layoutParams.height)
        }
    }

    override fun onFling(p0: MotionEvent?, e1: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (p0 != null)
            if (abs(velocityX) > 200 && (p0.x - e1.x > 100 || e1.x - p0.x > 100))
                dismiss()

        return false
    }

    override fun onTouch(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                mDX = mPosition.x - e.rawX.toInt()
                mDY = mPosition.y - e.rawY.toInt()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val currTime = System.currentTimeMillis()
                if (currTime - mTouchParamUpdateTimer > 200) {
                    mTouchParamUpdateTimer = currTime
                    mPopup?.update(mPosition.x, mPosition.y, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                onUp(e)
                return true
            }
        }

        return false
    }

    override fun onResize(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                mDX = mFloatingView.layoutParams.width - e.rawX.toInt()
                mDY = mFloatingView.layoutParams.height - e.rawY.toInt()
                return true
            }
            MotionEvent.ACTION_UP -> {
                fixBoxBounds()
                mPopup?.update(mPosition.x, mPosition.y, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                onUp(e)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val currTime = System.currentTimeMillis()
                if (currTime - mSizeParamUpdateTimer > 200) {
                    mFloatingView.layoutParams.width = mDX + e.rawX.toInt()
                    mFloatingView.layoutParams.height = mDY + e.rawY.toInt()
                    fixBoxBounds()
                    mSizeParamUpdateTimer = currTime
                    mPopup?.update(mPosition.x, mPosition.y, mFloatingView.layoutParams.width, mFloatingView.layoutParams.height)
                }
                return true
            }
        }
        return false
    }

    private fun fixBoxBounds() {
        if (mPosition.x < 0)
            mPosition.x = 0
        else if (mPosition.x + mFloatingView.layoutParams.width > parent.width)
            mPosition.x = parent.width - mFloatingView.layoutParams.width

        if (mPosition.y < 0)
            mPosition.y = 0
        else if (mPosition.y + mFloatingView.layoutParams.height > parent.height)
            mPosition.y = parent.height - mFloatingView.layoutParams.height

        if (mFloatingView.layoutParams.width < 0)
            mFloatingView.layoutParams.width = mLastSize.width
        else if (mFloatingView.layoutParams.width > parent.width)
            mFloatingView.layoutParams.width = parent.width

        if (mFloatingView.layoutParams.height < 0)
            mFloatingView.layoutParams.height = mLastSize.height
        else if (mFloatingView.layoutParams.height > parent.height)
            mFloatingView.layoutParams.height = parent.height

        if (mFloatingView.layoutParams.width < minSize)
            mFloatingView.layoutParams.width = minSize

        if (mFloatingView.layoutParams.height < minSize)
            mFloatingView.layoutParams.height = minSize

        mLastSize = Size(mFloatingView.layoutParams.width, mFloatingView.layoutParams.height)
    }

    fun show() {
        dismiss()
        isShowing = true
        mPopup?.showAtLocation(parent, Gravity.NO_GRAVITY, mPosition.x, mPosition.y)
    }

    fun dismiss() {
        if (isShowing) {
            mPopup?.dismiss()
            isShowing = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (mHandler.hasCallbacks(mDismissCloseButton))
                    mHandler.removeCallbacks(mDismissCloseButton)
            } else
                mHandler.removeCallbacks(mDismissCloseButton)

            mCloseButton.visibility = View.GONE
        }
    }

    fun destroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mHandler.hasCallbacks(mDismissCloseButton))
                mHandler.removeCallbacks(mDismissCloseButton)
        } else
            mHandler.removeCallbacks(mDismissCloseButton)

        dismiss()
    }

    fun processTesseractAsync() {
        val language = (activity as OcrProcess).getLanguage() ?: return

        Toast.makeText(
            context,
            context.resources.getString(R.string.ocr_tesseract_get_request),
            Toast.LENGTH_SHORT
        ).show()

        try {
            val location = IntArray(2)
            mFloatingView.getLocationOnScreen(location)
            val image = (activity as OcrProcess).getImage(location[0], location[1], mFloatingView.width, mFloatingView.height) ?: return
            Tesseract.getInstance(context).processAsync(language, image) {
                (activity as OcrProcess).setText(it)
            }
        } catch (e: Exception) {
            mLOGGER.error("Error when start async process tesseract: " + e.message, e)
        }
    }

    fun processTesseract(languages: Languages): String? {
        return try {
            val location = IntArray(2)
            mFloatingView.getLocationOnScreen(location)
            val image = (activity as OcrProcess).getImage(location[0], location[1], mFloatingView.width, mFloatingView.height) ?: return null
            val tess = Tesseract.getInstance(context)
            tess.process(languages, image)
        } catch (e: Exception) {
            mLOGGER.error("Error when process tesseract: " + e.message, e)
            null
        }
    }

}