package br.com.fenix.bilingualreader.view.ui.window

import android.content.Context
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageButton
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.ImageLoadType
import br.com.fenix.bilingualreader.service.controller.SubTitleController
import br.com.fenix.bilingualreader.service.ocr.OcrProcess
import br.com.fenix.bilingualreader.view.ui.reader.manga.MangaReaderActivity
import kotlin.math.abs


class FloatingButtons constructor(
    private val context: Context,
    private val activity: AppCompatActivity,
    private val parent : View
) {

    private var mFloatingView: View = LayoutInflater.from(context).inflate(R.layout.floating_manga_buttons, null)

    private var windowManager: PopupWindow? = null
        get() {
            if (field == null) {
                field = PopupWindow(parent, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                field?.contentView = mFloatingView
                field?.isFocusable = false
                field?.isOutsideTouchable = false
                field?.setOnDismissListener { isShowing = false }
            }
            return field
        }

    private var mLastX: Int = 0
    private var mLastY: Int = 0
    private var mFirstX: Int = 0
    private var mFirstY: Int = 0

    var isShowing = false

    private val mOnFlingListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (e1 != null)
                if (abs(e1.x - e2.x) > 150) {
                    if (e2.x > e1.x)
                        moveWindow(false)
                    else if (e2.x < e1.x)
                        moveWindow(true)
                    return false
                }

            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    private fun moveWindow(toLeft: Boolean) {
        inLeft = if (toLeft)
            false
        else
            true
        onMove()
    }

    private val mOnFlingDetector = GestureDetector(context, mOnFlingListener)

    private val onTouchListener = View.OnTouchListener { view, event ->
        view.performClick()
        mOnFlingDetector.onTouchEvent(event)
        val totalDeltaX = mLastX - mFirstX
        val totalDeltaY = mLastY - mFirstY

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mLastX = event.rawX.toInt()
                mLastY = event.rawY.toInt()
                mFirstX = mLastX
                mFirstY = mLastY
            }
            MotionEvent.ACTION_UP -> {
                //view.performClick()
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX.toInt() - mLastX
                val deltaY = event.rawY.toInt() - mLastY
                mLastX = event.rawX.toInt()
                mLastY = event.rawY.toInt()
                if (abs(totalDeltaX) >= 5 || abs(totalDeltaY) >= 5) {
                    if (event.pointerCount == 1) {
                        mPosition.x += deltaX
                        mPosition.y += deltaY
                        windowManager?.update(mPosition.x, mPosition.y, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    }

                    if (mPosition.x > mMiddle && inLeft) {
                        inLeft = false
                        mMoveWindow.setImageDrawable(mIconToLeft)
                    } else if (mPosition.x < mMiddle && !inLeft) {
                        inLeft = true
                        mMoveWindow.setImageDrawable(mIconToRight)
                    }
                }
            }
            else -> {}
        }
        true
    }

    private var mSubTitleController = SubTitleController.getInstance(context)
    private var mContent: LinearLayout
    private var mMoveWindow: AppCompatImageButton
    private var mIconToRight: Drawable?
    private var mIconToLeft: Drawable?
    private var mPosition : Point

    private val mMiddle: Int
    private var inLeft = true

    init {
        with(mFloatingView) {
            mContent = this.findViewById(R.id.floating_manga_buttons_content)

            this.findViewById<AppCompatImageButton>(R.id.floating_manga_buttons_close)
                .setOnClickListener { dismiss() }
            this.findViewById<AppCompatImageButton>(R.id.floating_manga_buttons_page_linked)
                .setOnClickListener { mSubTitleController.drawPageLinked() }
            this.findViewById<AppCompatImageButton>(R.id.floating_manga_buttons_ocr_google_vision)
                .setOnClickListener { mSubTitleController.drawOcrPage(ImageLoadType.OCR, activity as OcrProcess) }
            this.findViewById<AppCompatImageButton>(R.id.floating_manga_buttons_draw_text)
                .setOnClickListener { mSubTitleController.drawSelectedText() }
            this.findViewById<AppCompatImageButton>(R.id.floating_manga_buttons_floating_window)
                .setOnClickListener { (activity as MangaReaderActivity).openFloatingSubtitle() }
            this.findViewById<AppCompatImageButton>(R.id.floating_manga_buttons_file_link)
                .setOnClickListener { (activity as MangaReaderActivity).openFileLink() }
            mMoveWindow = this.findViewById(R.id.floating_manga_buttons_move_window)
            mMoveWindow.setOnClickListener { onMove() }

            mIconToRight = AppCompatResources.getDrawable(context, R.drawable.ico_floating_button_change_right)
            mIconToLeft = AppCompatResources.getDrawable(context, R.drawable.ico_floating_button_change_left)
        }

        mFloatingView.setOnTouchListener(onTouchListener)

        mMiddle = parent.width / 2
        inLeft = true

        mMoveWindow.setImageDrawable(mIconToRight)
        inLeft = true
        mPosition = Point(10, mMiddle)
    }

    private fun onMove() {
        if (inLeft) {
            mMoveWindow.setImageDrawable(mIconToLeft)
            inLeft = false
            mPosition.x = parent.width - (mContent.width + 10)
            windowManager?.update(mPosition.x, mPosition.y, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        } else {
            mMoveWindow.setImageDrawable(mIconToRight)
            inLeft = true
            mPosition.x = 10
            windowManager?.update(mPosition.x, mPosition.y, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    fun show() {
        dismiss()
        isShowing = true
        mPosition = Point(10, parent.width / 2)
        windowManager?.showAtLocation(parent, Gravity.NO_GRAVITY, mPosition.x, mPosition.y)
    }

    fun dismiss() {
        if (isShowing) {
            windowManager?.dismiss()
            isShowing = false
        }
    }

    fun destroy() {
        dismiss()
    }

}