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

    /** Cached bitmaps — rebuilt only when the drawable changes */
    private var mDotPrimaryBitmap: android.graphics.Bitmap? = null
    private var mDotSecondaryBitmap: android.graphics.Bitmap? = null

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
        mDotPrimaryBitmap = mDotPrimaryMark?.toBitmap()
        invalidate()
    }

    /**
     * @param dotsResource resource id to be used for dots drawing
     */
    fun setDotsSecondaryDrawable(dotsResource: Int) {
        mDotSecondaryMark = resources.getDrawable(dotsResource, context.theme)
        mDotSecondaryBitmap = mDotSecondaryMark?.toBitmap()
        invalidate()
    }

    private fun drawDots(canvas: Canvas, positions: IntArray, mark: Drawable?, cachedBitmap: android.graphics.Bitmap?) {
        if (positions.isEmpty() || mark == null)
            return

        val image = cachedBitmap ?: mark.toBitmap()

        val trackWidth = (measuredWidth - paddingLeft - paddingRight - thumb.intrinsicWidth).toFloat()
        val startX = (paddingLeft + thumb.intrinsicWidth / 2f)
        val top = paddingTop + (measuredHeight - paddingTop - paddingBottom - mark.intrinsicHeight) / 2f

        val range = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            (max - min).toFloat()
        else
            (max - MIN).toFloat()

        for (position in positions) {
            val scale: Float = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                if (range > 0) (position - min) / range else 0f
            else
                if (range > 0) (position - MIN) / range else 0f

            val dotX = startX + (trackWidth * scale) + 0.5f
            canvas.drawBitmap(image, dotX - (image.width / 2f), top, null)
        }
    }

    @Synchronized
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawDots(canvas, mDotsPrimaryPositions, mDotPrimaryMark, mDotPrimaryBitmap)
        drawDots(canvas, mDotsSecondaryPositions, mDotSecondaryMark, mDotSecondaryBitmap)

        canvas.withTranslation((paddingLeft - thumbOffset).toFloat(), paddingTop.toFloat()) {
            thumb.draw(canvas)
        }
    }
}