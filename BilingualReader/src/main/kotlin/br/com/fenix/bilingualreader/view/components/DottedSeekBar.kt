package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.graphics.drawable.toBitmap
import br.com.fenix.bilingualreader.R


/**
 * Seek bar with dots on it on specific time / percent
 */
class DottedSeekBar : AppCompatSeekBar {

    //Used only android < Oreo
    private val MIN: Int = 0

    /** Int values which corresponds to dots  */
    private var mDots: IntArray = intArrayOf()
    private var mDotsInversed: IntArray = intArrayOf()
    private var mDotsPositions: IntArray = intArrayOf()
    private var isInverse = false

    /** Drawable for dot  */
    private var mDotMark: Drawable? = null

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(attrs)
    }

    /**
     * Initializes Seek bar extended attributes from xml
     *
     * @param attributeSet [AttributeSet]
     */
    private fun init(attributeSet: AttributeSet?) {
        val attrsArray = context.obtainStyledAttributes(attributeSet, R.styleable.DottedSeekBar, 0, 0)
        val dotsArrayResource = attrsArray.getResourceId(R.styleable.DottedSeekBar_dots_positions, 0)
        if (0 != dotsArrayResource)
            mDotsPositions = resources.getIntArray(dotsArrayResource)

        val dotDrawableId = attrsArray.getResourceId(R.styleable.DottedSeekBar_dots_drawable, 0)
        if (0 != dotDrawableId)
            mDotMark = resources.getDrawable(dotDrawableId, context.theme)
    }

    /**
     * @param dots to be displayed on this SeekBar
     */
    fun setDots(dots: IntArray, inverse: IntArray) {
        mDots = dots
        mDotsInversed = inverse
        mDotsPositions = mDots.clone()
        isInverse = false
        invalidate()
    }

    /**
     * @param isInverse used in manga mode
     */
    fun setDotsMode(isInverse: Boolean) {
        if (this.isInverse != isInverse) {
            if (isInverse)
                mDotsPositions = mDotsInversed.clone()
            else
                mDotsPositions = mDots.clone()
            this.isInverse = isInverse
            invalidate()
        }
    }


    /**
     * @param dotsResource resource id to be used for dots drawing
     */
    fun setDotsDrawable(dotsResource: Int) {
        mDotMark = resources.getDrawable(dotsResource, context.theme)
        invalidate()
    }

    @Synchronized
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mDotsPositions.isNotEmpty() && mDotMark != null) {
            val w: Int = mDotMark!!.intrinsicWidth
            val h: Int = mDotMark!!.intrinsicHeight
            val halfW = if (w >= 0) w / 2 else 1
            val halfH = if (h >= 0) h / 2 else 1
            mDotMark!!.setBounds(-halfW, -halfH, halfW, halfH)

            val top = paddingTop + (measuredHeight - paddingTop - paddingBottom) / 2 - (h / 2f)
            val padding = paddingLeft - thumbOffset + (thumb.intrinsicWidth / 4f)

            val range = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                (max - min).toFloat()
            else
                (max - MIN).toFloat()

            val available = (measuredWidth - paddingLeft - paddingRight)
            val image = mDotMark!!.toBitmap()
            for (position in mDotsPositions) {
                val scale: Float = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    if (range > 0) (position - min) / range else 0f
                else
                    if (range > 0) (position - MIN) / range else 0f

                val step = (available * scale + 0.5f)
                canvas.drawBitmap(image, padding + step, top, null)
            }
        }
    }
}