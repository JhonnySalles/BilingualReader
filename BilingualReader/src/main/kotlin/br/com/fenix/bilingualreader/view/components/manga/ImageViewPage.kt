package br.com.fenix.bilingualreader.view.components.manga

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.OverScroller
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.ViewCompat
import androidx.core.view.drawToBitmap
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.ReaderMode
import br.com.fenix.bilingualreader.model.interfaces.BaseImageView
import br.com.fenix.bilingualreader.service.functions.AutoScroll
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import org.slf4j.LoggerFactory
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign


open class ImageViewPage(context: Context, attributeSet: AttributeSet?) : AppCompatImageView(context, attributeSet), AutoScroll, BaseImageView {

    companion object {
        const val ZOOM_DURATION = 200
        const val SCROLL_DURATION = 300
    }

    constructor(context: Context) : this(context, null)

    private val mLOGGER = LoggerFactory.getLogger(ImageViewPage::class.java)

    private var mViewMode: ReaderMode
    private var mHaveFrame = false
    private var mSkipScaling = false
    private var mTranslateRightEdge = false
    private var mOuterTouchListener: OnTouchListener? = null
    private var mScaleGestureDetector: ScaleGestureDetector
    private var mDragGestureDetector: GestureDetector
    private var mScroller: OverScroller
    private var mMinScale = 0F
    private var mMaxScale = 0F
    private var mZoomScale = 0F
    private var mOriginalScale = 0F
    private val mValues = FloatArray(9)
    private var mMatrix: Matrix = Matrix()

    var useMagnifierType = false
    private var mPinch = false
    private var mMagnifierMatrix: Matrix = Matrix()
    private var mZoomPos: PointF
    private var mZooming = false
    private var mLastZoomPos: PointF
    private var mPaint: Paint
    private var mBorder: Paint
    private var mBackground: Paint
    private lateinit var mBitmap: Bitmap
    private lateinit var mShader: BitmapShader
    private val mMagnifierScale = 2.5F
    private val mMagnifierCenter: Float
    private val mMagnifierSize: Float
    private val mMagnifierRadius: Float
    private val mRightZoomScale: PointF

    private var mTouchSlop = 0
    private var mInitialX = 0f
    private var mInitialY = 0f
    private val mParentViewPager: ViewPager2?
        get() {
            var v: View? = parent as? View
            while (v != null && v !is ViewPager2) {
                v = v.parent as? View
            }
            return v as? ViewPager2
        }

    fun setViewMode(viewMode: ReaderMode) {
        mViewMode = viewMode
        mSkipScaling = false
        requestLayout()
        invalidate()
    }

    override fun setFrame(l: Int, t: Int, r: Int, b: Int): Boolean {
        val changed: Boolean = super.setFrame(l, t, r, b)
        mHaveFrame = true
        scale()
        return changed
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        mSkipScaling = false
        scale()
    }

    init {
        scaleType = ScaleType.MATRIX
        imageMatrix = mMatrix
        mScaleGestureDetector = ScaleGestureDetector(getContext(), PrivateScaleDetector())
        mDragGestureDetector = GestureDetector(getContext(), PrivateDragListener())
        super.setOnTouchListener { v, event ->
            v.performClick()

            mPinch = event.pointerCount > 1
            if (mPinch) {
                mScaleGestureDetector.onTouchEvent(event)
                parent.requestDisallowInterceptTouchEvent(true)
            } else
                mDragGestureDetector.onTouchEvent(event)

            if (mZooming)
                parent.requestDisallowInterceptTouchEvent(true)

            if (!mZooming && !mPinch)
                validScrollingTouchEventInViewPager(event)

            mOuterTouchListener?.onTouch(v, event)
            onTouchEvent(event)
            true
        }

        mScroller = OverScroller(context)
        mScroller.setFriction(ViewConfiguration.getScrollFriction() * 2)
        mViewMode = ReaderMode.FIT_WIDTH

        val isTablet = resources.getBoolean(R.bool.isTablet)
        mMagnifierSize = if (isTablet) resources.getDimension(R.dimen.reader_zoom_tablet_size) else resources.getDimension(
                R.dimen.reader_zoom_size
            )
        mMagnifierRadius = if (isTablet) resources.getDimension(R.dimen.reader_zoom_magnifier_tablet_size) else resources.getDimension(
                R.dimen.reader_zoom_magnifier_size
            )
        mMagnifierCenter = mMagnifierSize / 2
        mZoomPos = PointF(0F, 0F)
        mLastZoomPos = PointF(-1F, -1F)
        mPaint = Paint()

        mBorder = Paint()
        mBorder.color = context.getColor(R.color.magnifier_border)
        mBorder.style = Paint.Style.STROKE
        mBorder.strokeWidth = resources.getDimension(R.dimen.reader_zoom_border)

        mBackground = Paint()
        mBackground.color = context.getColorFromAttr(R.attr.colorSurface)
        mBackground.style = Paint.Style.FILL

        val displayMetrics = Resources.getSystem().displayMetrics
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        mRightZoomScale = if (isLandscape) PointF(displayMetrics.widthPixels.toFloat() * displayMetrics.density, 0f)  else PointF(displayMetrics.heightPixels.toFloat(), 0f)
    }

    override fun isVisible(): Boolean = visibility == VISIBLE

    override fun autoScroll(isBack: Boolean): Boolean {
        val displayMetrics = Resources.getSystem().displayMetrics

        val distance = if (isBack)
            mValues[Matrix.MTRANS_Y] + (displayMetrics.heightPixels).toFloat()
        else
            mValues[Matrix.MTRANS_Y] - (displayMetrics.heightPixels).toFloat()

        val imageSize = computeCurrentImageSize()
        val imageHeight = imageSize.y
        mMatrix.getValues(mValues)

        val isScroll = if (imageHeight < height)
            true
        else if (isBack)
            mValues[Matrix.MTRANS_Y] >= 0F
        else if (imageHeight > height)
            (mValues[Matrix.MTRANS_Y] * -1) >= (imageHeight - height).toFloat()
        else
            (mValues[Matrix.MTRANS_Y] * -1) >= (height / 2 - imageHeight / 2).toFloat()

        post(ScrollAnimation(0F, mValues[Matrix.MTRANS_Y], 0F, distance))

        return !isScroll
    }

    private fun getMultipleScale(): Float {
        val dWidth = drawable.intrinsicWidth
        val dHeight = drawable.intrinsicHeight
        val vWidth: Int = width
        val vHeight: Int = height
        val current = getCurrentScale()

        val heightRatio = vHeight.toFloat() / dHeight
        val w = dWidth * heightRatio
        return if (w < vWidth)
            current * dWidth / max(dWidth, vWidth)
        else
            current * dHeight / max(dHeight, vHeight)
    }

    private fun generateScale(multiple: Float): Float {
        val dWidth = drawable.intrinsicWidth
        val dHeight = drawable.intrinsicHeight
        val vWidth: Int = width
        val vHeight: Int = height

        val heightRatio = vHeight.toFloat() / dHeight
        val w = dWidth * heightRatio
        return if (w < vWidth)
            max(dWidth, vWidth) * multiple / dWidth
        else
            max(dHeight, vHeight) * multiple / dHeight
    }

    override val isApplyPercent: Boolean = true
    override fun getScrollPercent(): Triple<Float, Float, Float> {
        val imageSize = computeCurrentImageSize()
        return Triple(
            (mValues[Matrix.MTRANS_X] / imageSize.x),
            (mValues[Matrix.MTRANS_Y] / imageSize.y),
            getMultipleScale()
        )
    }

    override fun setScrollPercent(percent: Triple<Float, Float, Float>) {
        val (x, y, zoom) = percent
        val scale = generateScale(zoom)
        mMatrix.setScale(scale, scale)
        mMatrix.getValues(mValues)
        val imageSize = computeCurrentImageSize()
        val posY = mValues[Matrix.MTRANS_Y] + (imageSize.y * y)
        val posX = mValues[Matrix.MTRANS_X] + (imageSize.x * x)
        mMatrix.postTranslate(posX, posY)
        imageMatrix = mMatrix
        postInvalidate()
    }

    override fun setOnTouchListener(listener: OnTouchListener?) {
        mOuterTouchListener = listener
    }

    fun setTranslateToRightEdge(translate: Boolean) {
        mTranslateRightEdge = translate
    }

    open fun scale() {
        if (drawable == null || !mHaveFrame || mSkipScaling) return
        val dWidth = drawable.intrinsicWidth
        val dHeight = drawable.intrinsicHeight
        val vWidth: Int = width
        val vHeight: Int = height
        when {
            mViewMode === ReaderMode.ASPECT_FILL -> {
                val scale: Float
                var dx = 0f
                if (dWidth * vHeight > vWidth * dHeight) {
                    scale = vHeight.toFloat() / dHeight.toFloat()
                    if (mTranslateRightEdge) dx = vWidth - dWidth * scale
                } else {
                    scale = vWidth.toFloat() / dWidth.toFloat()
                }
                mMatrix.setScale(scale, scale)
                mMatrix.postTranslate((dx + 0.5f), 0f)
            }
            mViewMode === ReaderMode.ASPECT_FIT -> {
                val mTempSrc = RectF(0F, 0F, dWidth.toFloat(), dHeight.toFloat())
                val mTempDst = RectF(0F, 0F, vWidth.toFloat(), vHeight.toFloat())
                mMatrix.setRectToRect(mTempSrc, mTempDst, Matrix.ScaleToFit.CENTER)
            }
            mViewMode === ReaderMode.FIT_WIDTH -> {
                val widthScale = width.toFloat() / drawable.intrinsicWidth
                mMatrix.setScale(widthScale, widthScale)
                mMatrix.postTranslate(0f, 0f)
            }
        }

        // calculate min/max scale
        val heightRatio = vHeight.toFloat() / dHeight
        val w = dWidth * heightRatio
        if (w < vWidth) {
            mMinScale = vHeight * 0.75f / dHeight
            mMaxScale = max(dWidth, vWidth) * 4f / dWidth
            mZoomScale = max(dWidth, vWidth) * 2f / dWidth
        } else {
            mMinScale = vWidth * 0.75f / dWidth
            mMaxScale = max(dHeight, vHeight) * 4f / dHeight
            mZoomScale = max(dHeight, vHeight) * 2f / dHeight
        }
        imageMatrix = mMatrix
        mOriginalScale = getCurrentScale()
        mSkipScaling = true
    }

    inner class PrivateScaleDetector : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mMatrix.getValues(mValues)
            val scale = mValues[Matrix.MSCALE_X]
            var scaleFactor = detector.scaleFactor
            val scaleNew = scale * scaleFactor
            var scalable = true
            if (scaleFactor > 1 && mMaxScale - scaleNew < 0) {
                scaleFactor = mMaxScale / scale
                scalable = false
            } else if (scaleFactor < 1 && mMinScale - scaleNew > 0) {
                scaleFactor = mMinScale / scale
                scalable = false
            }
            mMatrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
            imageMatrix = mMatrix
            return scalable
        }
    }

    inner class PrivateDragListener : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            mScroller.forceFinished(true)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            mMatrix.postTranslate(-distanceX, -distanceY)
            imageMatrix = mMatrix
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            val imageSize: Point = computeCurrentImageSize()
            val offset: Point = computeCurrentOffset()
            var minX: Int = -imageSize.x - this@ImageViewPage.width
            var minY: Int = -imageSize.y - this@ImageViewPage.height
            var maxX = 0
            var maxY = 0
            if (offset.x > 0) {
                minX = offset.x
                maxX = offset.x
            }
            if (offset.y > 0) {
                minY = offset.y
                maxY = offset.y
            }
            mScroller.fling(offset.x, offset.y, velocityX.toInt(), velocityY.toInt(), minX, maxX, minY, maxY)
            ViewCompat.postInvalidateOnAnimation(this@ImageViewPage)
            return true
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            if (e.action == MotionEvent.ACTION_UP) {
                val scale = if (mOriginalScale == getCurrentScale()) mZoomScale else mOriginalScale
                zoomAnimated(e, scale)
            }
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            mBitmap = this@ImageViewPage.drawToBitmap()
            mShader = BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            mPaint = Paint()
            mZooming = true
            mLastZoomPos = PointF(-1F, -1F)
            this@ImageViewPage.invalidate()
        }
    }

    open fun zoomAnimated(e: MotionEvent, scale: Float) {
        post(ZoomAnimation(e.x, e.y, scale))
    }

    open fun zoomAnimated(scale: Float, isLeftToRight: Boolean) {
        if (!isLeftToRight)
            post(ZoomAnimation(mRightZoomScale.x, mRightZoomScale.y, scale))
        else
            post(ZoomAnimation(0f, 0f, scale))
    }

    override fun computeScroll() {
        if (!mScroller.isFinished && mScroller.computeScrollOffset()) {
            val curX = mScroller.currX
            val curY = mScroller.currY
            mMatrix.getValues(mValues)
            mValues[Matrix.MTRANS_X] = curX.toFloat()
            mValues[Matrix.MTRANS_Y] = curY.toFloat()
            mMatrix.setValues(mValues)
            imageMatrix = mMatrix
            ViewCompat.postInvalidateOnAnimation(this)
        }
        super.computeScroll()
    }

    override fun getPointerCoordinate(e: MotionEvent): FloatArray {
        val index = e.actionIndex
        val coordinates = floatArrayOf(e.getX(index), e.getY(index))
        val matrix = Matrix()
        imageMatrix.invert(matrix)
        mValues[Matrix.MTRANS_X] = mScroller.currX.toFloat()
        mValues[Matrix.MTRANS_Y] = mScroller.currY.toFloat()
        matrix.mapPoints(coordinates)
        val imageSize = computeCurrentImageSize()
        return floatArrayOf(
            coordinates[0],
            coordinates[1],
            imageSize.x.toFloat(),
            imageSize.y.toFloat()
        )
    }

    open fun getCurrentScale(): Float {
        mMatrix.getValues(mValues)
        return mValues[Matrix.MSCALE_X]
    }

    open fun computeCurrentImageSize(): Point {
        val size = Point()
        val d: Drawable = drawable ?: return size
        mMatrix.getValues(mValues)
        val scale = mValues[Matrix.MSCALE_X]
        val width = d.intrinsicWidth * scale
        val height = d.intrinsicHeight * scale
        size[width.toInt()] = height.toInt()
        return size
    }

    open fun computeCurrentOffset(): Point {
        val offset = Point()
        mMatrix.getValues(mValues)
        val transX = mValues[Matrix.MTRANS_X]
        val transY = mValues[Matrix.MTRANS_Y]
        offset[transX.toInt()] = transY.toInt()
        return offset
    }

    override fun setImageMatrix(matrix: Matrix) {
        super.setImageMatrix(fixMatrix(matrix))
        postInvalidate()
    }

    open fun fixMatrix(matrix: Matrix): Matrix {
        if (drawable == null) return matrix
        matrix.getValues(mValues)
        val imageSize = computeCurrentImageSize()
        val imageWidth = imageSize.x
        val imageHeight = imageSize.y
        val maxTransX: Float = (imageWidth - width).toFloat()
        val maxTransY: Float = (imageHeight - height).toFloat()

        if (imageWidth > width)
            mValues[Matrix.MTRANS_X] = min(0F, max(mValues[Matrix.MTRANS_X], -maxTransX))
        else
            mValues[Matrix.MTRANS_X] = (width / 2 - imageWidth / 2).toFloat()

        if (imageHeight > height)
            mValues[Matrix.MTRANS_Y] = min(0F, max(mValues[Matrix.MTRANS_Y], -maxTransY))
        else
            mValues[Matrix.MTRANS_Y] = (height / 2 - imageHeight / 2).toFloat()

        matrix.setValues(mValues)
        return matrix
    }

    private fun canChildScroll(orientation: Int, delta: Float): Boolean {
        val direction = -delta.sign.toInt()
        return when (orientation) {
            0 -> canScrollHorizontally(direction)
            1 -> canScrollVertically(direction)
            else -> throw IllegalArgumentException()
        }
    }

    private fun validScrollingTouchEventInViewPager(e: MotionEvent) {
        val orientation = mParentViewPager?.orientation ?: return

        // Early return if child can't scroll in same direction as parent
        if (!canChildScroll(orientation, -1f) && !canChildScroll(orientation, 1f))
            return


        if (e.action == MotionEvent.ACTION_DOWN) {
            mInitialX = e.x
            mInitialY = e.y
            parent.requestDisallowInterceptTouchEvent(true)
        } else if (e.action == MotionEvent.ACTION_MOVE) {
            val dx = e.x - mInitialX
            val dy = e.y - mInitialY
            val isVpHorizontal = orientation == ORIENTATION_HORIZONTAL

            // assuming ViewPager2 touch-slop is 2x touch-slop of child
            val scaledDx = dx.absoluteValue * if (isVpHorizontal) .5f else 1f
            val scaledDy = dy.absoluteValue * if (isVpHorizontal) 1f else .5f

            if (scaledDx > mTouchSlop || scaledDy > mTouchSlop) {
                if (isVpHorizontal == (scaledDy > scaledDx))
                    parent.requestDisallowInterceptTouchEvent(false)
                else {
                    if (canChildScroll(orientation, if (isVpHorizontal) dx else dy))
                        parent.requestDisallowInterceptTouchEvent(true)
                    else
                        parent.requestDisallowInterceptTouchEvent(false)
                }
            }
        }
    }

    override fun canScrollVertically(direction: Int): Boolean {
        if (drawable == null)
            return false

        val imageHeight = computeCurrentImageSize().y.toFloat()
        val offsetY = computeCurrentOffset().x.toFloat()
        if (offsetY >= 0 && direction < 0)
            return false
        else if (abs(offsetY) + height >= imageHeight && direction > 0)
            return false

        return true
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        if (drawable == null)
            return false

        val imageWidth = computeCurrentImageSize().x.toFloat()
        val offsetX = computeCurrentOffset().x.toFloat()
        if (offsetX >= 0 && direction < 0)
            return false
        else if (abs(offsetX) + width >= imageWidth && direction > 0)
            return false

        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        performClick()
        val action = event?.action ?: return true

        mZoomPos.x = event.x
        mZoomPos.y = event.y

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (mZooming)
                    this.invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mLastZoomPos = PointF(-1F, -1F)
                mZooming = false
                this.invalidate()
            }
            else -> { }
        }

        return true
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        super.onDraw(canvas)
        if (mZooming && !mPinch) {
            mPaint.shader = mShader
            mMagnifierMatrix.set(mMatrix)

            if (useMagnifierType) {
                mMagnifierMatrix.reset()
                mMagnifierMatrix.postScale(mMagnifierScale, mMagnifierScale, mZoomPos.x, mZoomPos.y)
                mPaint.shader.setLocalMatrix(mMagnifierMatrix)
                canvas.drawCircle(mZoomPos.x, mZoomPos.y, mMagnifierRadius, mPaint)
            } else {
                val x = if (mZoomPos.x < (width / 2)) width.minus(mMagnifierSize) else 0F

                if (mLastZoomPos.x != x)
                    mLastZoomPos.y = if (mZoomPos.y < (height / 2)) height.minus(mMagnifierSize) else 0F

                mLastZoomPos.x = x

                mMagnifierMatrix.reset()
                mMagnifierMatrix.postScale(mMagnifierScale, mMagnifierScale, mZoomPos.x, mZoomPos.y)
                mMagnifierMatrix.postTranslate(-mZoomPos.x, -mZoomPos.y)
                mMagnifierMatrix.postTranslate(mMagnifierCenter, mMagnifierCenter)
                mMagnifierMatrix.postTranslate(mLastZoomPos.x, mLastZoomPos.y)
                mPaint.shader.setLocalMatrix(mMagnifierMatrix)

                canvas.drawRect(
                    mLastZoomPos.x - 1,
                    mLastZoomPos.y - 2,
                    mLastZoomPos.x + mMagnifierSize + 1,
                    mLastZoomPos.y + mMagnifierSize + 1,
                    mBorder
                )

                canvas.drawRect(
                    mLastZoomPos.x,
                    mLastZoomPos.y,
                    mLastZoomPos.x + mMagnifierSize,
                    mLastZoomPos.y + mMagnifierSize,
                    mBackground
                )

                canvas.drawRect(
                    mLastZoomPos.x,
                    mLastZoomPos.y,
                    mLastZoomPos.x + mMagnifierSize,
                    mLastZoomPos.y + mMagnifierSize,
                    mPaint
                )
            }
        }
        canvas.restore()
    }

    inner class ZoomAnimation(x: Float, y: Float, scale: Float) : Runnable {
        private var mX: Float
        private var mY: Float
        private var mScale: Float
        private var mInterpolator: Interpolator
        private var mStartScale: Float
        private var mStartTime: Long
        override fun run() {
            var t = (System.currentTimeMillis() - mStartTime).toFloat() / ZOOM_DURATION
            val interpolateRatio = mInterpolator.getInterpolation(t)
            t = if (t > 1f) 1f else t
            mMatrix.getValues(mValues)
            val newScale = mStartScale + interpolateRatio * (mScale - mStartScale)
            val newScaleFactor = newScale / mValues[Matrix.MSCALE_X]
            mMatrix.postScale(newScaleFactor, newScaleFactor, mX, mY)
            imageMatrix = mMatrix
            if (t < 1f) {
                post(this)
            } else {
                // set exact scale
                mMatrix.getValues(mValues)
                mMatrix.setScale(mScale, mScale)
                mMatrix.postTranslate(mValues[Matrix.MTRANS_X], mValues[Matrix.MTRANS_Y])
                setImageMatrix(mMatrix)
            }
        }

        init {
            mMatrix.getValues(mValues)
            mX = x
            mY = y
            mScale = scale
            mInterpolator = AccelerateDecelerateInterpolator()
            mStartScale = getCurrentScale()
            mStartTime = System.currentTimeMillis()
        }
    }

    inner class ScrollAnimation(xInitial: Float, yInitial: Float, xFinal: Float, yFinal: Float) : Runnable {
        private var mYInitial: Float = yInitial
        private var mXInitial: Float = xInitial
        private var mYFinal: Float = yFinal
        private var mXFinal: Float = xFinal
        private var mInterpolator: Interpolator = AccelerateDecelerateInterpolator()
        private var mStartTime: Long = System.currentTimeMillis()
        private var mInitialMatrix = Matrix(mMatrix)

        override fun run() {
            var t = (System.currentTimeMillis() - mStartTime).toFloat() / SCROLL_DURATION
            val interpolate = mInterpolator.getInterpolation(t)
            t = if (t > 1f) 1f else t

            val yTranslate = ((mYFinal - mYInitial) * interpolate)
            val xTranslate = ((mXFinal - mXInitial) * interpolate)

            mMatrix = Matrix(mInitialMatrix)
            mMatrix.postTranslate(xTranslate, yTranslate)
            imageMatrix = mMatrix
            if (t < 1f) {
                post(this)
            } else {
                // set exact scale
                mMatrix = Matrix(mInitialMatrix)
                mMatrix.postTranslate(mXFinal - mXInitial, mYFinal - mYInitial)
                setImageMatrix(mMatrix)
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}