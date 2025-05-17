package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.withTranslation
import br.com.fenix.bilingualreader.R


/**
 * Seek bar with dots on it on specific time / percent
 */
class DottedSeekBar : AppCompatSeekBar {

    //Used only android < Oreo
    private val MIN: Int = 0

    /** Int values which corresponds to dots  */
    private var mDotsPrimary: IntArray = intArrayOf()
    private var mDotsPrimaryInverse: IntArray = intArrayOf()
    private var mDotsPrimaryPositions: IntArray = intArrayOf()
    private var mDotsSecondary: IntArray = intArrayOf()
    private var mDotsSecondaryInverse: IntArray = intArrayOf()
    private var mDotsSecondaryPositions: IntArray = intArrayOf()
    private var isInverse = false

    /** Drawable for dot  */
    private var mDotPrimaryMark: Drawable? = null
    private var mDotSecondaryMark: Drawable? = null

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
        val dotsResourcePrimary = attrsArray.getResourceId(R.styleable.DottedSeekBar_dots_positions_primary, 0)
        if (0 != dotsResourcePrimary)
            mDotsPrimaryPositions = resources.getIntArray(dotsResourcePrimary)

        val dotsResouceSecondary = attrsArray.getResourceId(R.styleable.DottedSeekBar_dots_positions_secondary, 0)
        if (0 != dotsResouceSecondary)
            mDotsSecondaryPositions = resources.getIntArray(dotsResouceSecondary)

        val dotDrawablePrimary = attrsArray.getResourceId(R.styleable.DottedSeekBar_dots_drawable_primary, 0)
        if (0 != dotDrawablePrimary)
            mDotPrimaryMark = resources.getDrawable(dotDrawablePrimary, context.theme)

        val dotDrawableSecondary = attrsArray.getResourceId(R.styleable.DottedSeekBar_dots_drawable_secondary, 0)
        if (0 != dotDrawableSecondary)
            mDotSecondaryMark = resources.getDrawable(dotDrawableSecondary, context.theme)
    }

    /**
     * @param dots to be displayed on this SeekBar
     */
    fun setDots(primaryDots: IntArray, primaryInverse: IntArray, secondaryDots: IntArray, secondaryInverse: IntArray) {
        mDotsPrimary = primaryDots
        mDotsPrimaryInverse = primaryInverse

        mDotsPrimaryPositions = if (isInverse)
            mDotsPrimaryInverse.clone()
        else
            mDotsPrimary.clone()

        mDotsSecondary = secondaryDots
        mDotsSecondaryInverse = secondaryInverse

        mDotsSecondaryPositions = if (isInverse)
            mDotsSecondaryInverse.clone()
        else
            mDotsSecondary.clone()

        invalidate()
    }

    /**
     * @param dots to be displayed on this SeekBar
     */
    fun setPrimaryDots(dots: IntArray, inverse: IntArray) {
        mDotsPrimary = dots
        mDotsPrimaryInverse = inverse

        mDotsPrimaryPositions = if (isInverse)
            mDotsPrimaryInverse.clone()
        else
            mDotsPrimary.clone()

        invalidate()
    }

    /**
     * @param dots to be displayed on this SeekBar
     */
    fun setSecondaryDots(dots: IntArray, inverse: IntArray) {
        mDotsSecondary = dots
        mDotsSecondaryInverse = inverse

        mDotsSecondaryPositions = if (isInverse)
            mDotsSecondaryInverse.clone()
        else
            mDotsSecondary.clone()

        invalidate()
    }

    /**
     * @param isInverse used in reader mode
     */
    fun setDotsMode(isInverse: Boolean) {
        if (this.isInverse != isInverse) {
            mDotsPrimaryPositions = if (isInverse)
                mDotsPrimaryInverse.clone()
            else
                mDotsPrimary.clone()

            mDotsSecondaryPositions = if (isInverse)
                mDotsSecondaryInverse.clone()
            else
                mDotsSecondary.clone()

            this.isInverse = isInverse
            invalidate()
        }
    }

    /**
     * @param dotsResource resource id to be used for dots drawing
     */
    fun setDotsPrimaryDrawable(dotsResource: Int) {
        mDotPrimaryMark = resources.getDrawable(dotsResource, context.theme)
        invalidate()
    }

    /**
     * @param dotsResource resource id to be used for dots drawing
     */
    fun setDotsSecondaryDrawable(dotsResource: Int) {
        mDotSecondaryMark = resources.getDrawable(dotsResource, context.theme)
        invalidate()
    }

    @Synchronized
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (mDotsPrimaryPositions.isNotEmpty() && mDotPrimaryMark != null) {
            val w: Int = mDotPrimaryMark!!.intrinsicWidth
            val h: Int = mDotPrimaryMark!!.intrinsicHeight
            val halfW = if (w >= 0) w / 2 else 1
            val halfH = if (h >= 0) h / 2 else 1
            mDotPrimaryMark!!.setBounds(-halfW, -halfH, halfW, halfH)

            val top = paddingTop + (measuredHeight - paddingTop - paddingBottom) / 2 - (h / 2f)
            val padding = paddingLeft - thumbOffset + (thumb.intrinsicWidth / 4f)

            val range = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                (max - min).toFloat()
            else
                (max - MIN).toFloat()

            val available = (measuredWidth - paddingLeft - paddingRight)
            val image = mDotPrimaryMark!!.toBitmap()
            for (position in mDotsPrimaryPositions) {
                val scale: Float = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    if (range > 0) (position - min) / range else 0f
                else
                    if (range > 0) (position - MIN) / range else 0f

                val step = (available * scale + 0.5f)
                canvas.drawBitmap(image, padding + step, top, null)
            }
        }

        if (mDotsSecondaryPositions.isNotEmpty() && mDotSecondaryMark != null) {
            val w: Int = mDotSecondaryMark!!.intrinsicWidth
            val h: Int = mDotSecondaryMark!!.intrinsicHeight
            val halfW = if (w >= 0) w / 2 else 1
            val halfH = if (h >= 0) h / 2 else 1
            mDotSecondaryMark!!.setBounds(-halfW, -halfH, halfW, halfH)

            val top = paddingTop + (measuredHeight - paddingTop - paddingBottom) / 2 - (h / 2f)
            val padding = paddingLeft - thumbOffset + (thumb.intrinsicWidth / 4f)

            val range = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                (max - min).toFloat()
            else
                (max - MIN).toFloat()

            val available = (measuredWidth - paddingLeft - paddingRight)
            val image = mDotSecondaryMark!!.toBitmap()
            for (position in mDotsSecondaryPositions) {
                val scale: Float = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    if (range > 0) (position - min) / range else 0f
                else
                    if (range > 0) (position - MIN) / range else 0f

                val step = (available * scale + 0.5f)
                canvas.drawBitmap(image, padding + step, top, null)
            }
        }

        canvas.withTranslation((paddingLeft - thumbOffset).toFloat(), paddingTop.toFloat()) {
            thumb.draw(canvas)
        }
    }
}