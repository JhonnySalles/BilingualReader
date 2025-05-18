package br.com.fenix.bilingualreader.view.components.manga

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.view.drawToBitmap
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import org.slf4j.LoggerFactory


@SuppressLint("ClickableViewAccessibility")
class ZoomRecyclerView : RecyclerView {

    companion object {
        private const val DEFAULT_SCALE_DURATION = 300
        private const val DEFAULT_SCALE_FACTOR = 1f
        private const val DEFAULT_MAX_SCALE_FACTOR = 3.0f
        private const val DEFAULT_MIN_SCALE_FACTOR = 0.5f
        private const val PROPERTY_SCALE = "scale"
        private const val PROPERTY_TRANX = "tranX"
        private const val PROPERTY_TRANY = "tranY"
        private const val INVALID_TOUCH_POSITION = -1f
    }

    interface OnSwipeOutListener {
        fun onSwipeOutAtStart()
        fun onSwipeOutAtEnd()
    }

    private val mLOGGER = LoggerFactory.getLogger(ZoomRecyclerView::class.java)

    // touch detector
    private var mScaleDetector: ScaleGestureDetector? = null
    private var mGestureDetector: GestureDetector? = null
    private var mSwipeOutListener: OnSwipeOutListener? = null

    // draw param
    private var mViewWidth = 0f
    private var mViewHeight = 0f
    private var mTranX = 0f
    private var mTranY = 0f
    private var mScaleFactor = 0f

    // touch param
    private var mActivePointerId = MotionEvent.INVALID_POINTER_ID
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f

    var isEnableZoom = true
    var useMagnifierType: Boolean = false
    var isZoom = false
    var isEnablePinchZoom = true
        set(value) {
            if (field != value) {
                field = value
                if (!isEnablePinchZoom && mScaleFactor != 1f) {
                    zoom(mScaleFactor, 1f)
                }
            }
        }


    private var mScaleAnimator : ValueAnimator? = null
    private var mScaleCenterX = 0f
    private var mScaleCenterY = 0f
    private var mMaxTranX = 0f
    private var mMaxTranY = 0f

    private var mMaxScaleFactor = 0f
    private var mMinScaleFactor = 0f
    private var mDefaultScaleFactor = 0f
    private var mScaleDuration = 0

    private var mPinch = false
    private var mZooming = false
    private var mZoomPos: PointF
    private var mLastZoomPos: PointF
    private val mMagnifierScale = 2.5F
    private val mMagnifierCenter: Float
    private val mMagnifierSize: Float
    private val mMagnifierRadius: Float
    private val mRightZoomScale: PointF
    private var mMagnifierMatrix: Matrix = Matrix()
    private var mPaint: Paint
    private var mBorder: Paint
    private var mBackground: Paint
    private lateinit var mBitmap: Bitmap
    private lateinit var mShader: BitmapShader

    init {
        mZoomPos = PointF(0F, 0F)
        mLastZoomPos = PointF(-1F, -1F)

        val isTablet = resources.getBoolean(R.bool.isTablet)
        mMagnifierSize = if (isTablet) resources.getDimension(R.dimen.reader_zoom_tablet_size) else resources.getDimension(R.dimen.reader_zoom_size)
        mMagnifierRadius = if (isTablet) resources.getDimension(R.dimen.reader_zoom_magnifier_tablet_size) else resources.getDimension(R.dimen.reader_zoom_magnifier_size)
        mMagnifierCenter = mMagnifierSize / 2

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

    constructor(context: Context?) : super(context!!) {
        init(null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context!!, attrs, defStyle) {
        init(attrs)
    }

    private fun init(attr: AttributeSet?) {
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        mGestureDetector = GestureDetector(context, GestureListener())
        if (attr != null) {
            val a = context.obtainStyledAttributes(attr, R.styleable.ZoomRecyclerView, 0, 0)
            mMinScaleFactor = a.getFloat(R.styleable.ZoomRecyclerView_min_scale, DEFAULT_MIN_SCALE_FACTOR)
            mMaxScaleFactor = a.getFloat(R.styleable.ZoomRecyclerView_max_scale, DEFAULT_MAX_SCALE_FACTOR)
            mDefaultScaleFactor = a.getFloat(R.styleable.ZoomRecyclerView_default_scale, DEFAULT_SCALE_FACTOR)
            mScaleFactor = mDefaultScaleFactor
            mScaleDuration = a.getInteger(R.styleable.ZoomRecyclerView_zoom_duration, DEFAULT_SCALE_DURATION)
            isEnablePinchZoom = a.getBoolean(R.styleable.ZoomRecyclerView_zoom_pinch_enabled, this.isEnablePinchZoom)
            isEnableZoom = a.getBoolean(R.styleable.ZoomRecyclerView_zoom_enabled, this.isEnableZoom)
            a.recycle()
        } else {
            //init param with default
            mMaxScaleFactor = DEFAULT_MAX_SCALE_FACTOR
            mMinScaleFactor = DEFAULT_MIN_SCALE_FACTOR
            mDefaultScaleFactor = DEFAULT_SCALE_FACTOR
            mScaleFactor = mDefaultScaleFactor
            mScaleDuration = DEFAULT_SCALE_DURATION
        }
    }

    fun calculateScroll(dy: Int): Int {
        if (dy == 0) return 0

        val isScrollingToBottom = dy > 0

        if (isScrollingToBottom) {
            val canDragViewToBottom = this.mTranY != this.mMaxTranY
            if (canDragViewToBottom) {
                return 0
            } else {
                return ((1f / mScaleFactor) * dy).toInt()
            }
        } else {
            val canDragViewToTop = this.mTranY < 0
            if (canDragViewToTop) {
                return 0
            } else {
                return ((1f / mScaleFactor) * dy).toInt()
            }
        }
    }

    fun setOnSwipeOutListener(listener: OnSwipeOutListener?) {
        mSwipeOutListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mViewWidth = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        mViewHeight = MeasureSpec.getSize(heightMeasureSpec).toFloat()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        mZoomPos.x = ev.x
        mZoomPos.y = ev.y
        mPinch = ev.pointerCount > 1

        if (isEnableZoom)
            when (ev.actionMasked) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mZooming = false
                    this.invalidate()
                }

                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    if (mZooming) {
                        this.invalidate()
                        return true
                    }
                }
            }

        mScaleDetector!!.onTouchEvent(ev)
        mGestureDetector!!.onTouchEvent(ev)

        if (isEnablePinchZoom && !mZooming) {
            val action = ev.actionMasked
            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    val pointerIndex = ev.actionIndex
                    val x = ev.getX(pointerIndex)
                    val y = ev.getY(pointerIndex)
                    mLastTouchX = x
                    mLastTouchY = y
                    mActivePointerId = ev.getPointerId(0)
                }

                MotionEvent.ACTION_MOVE -> {
                    try {
                        val pointerIndex = ev.findPointerIndex(mActivePointerId)
                        val x = ev.getX(pointerIndex)
                        val y = ev.getY(pointerIndex)
                        if (!isZoom && mScaleFactor > 1) {
                            val dx = x - mLastTouchX
                            val dy = y - mLastTouchY
                            setTranslateXY(mTranX + dx, mTranY + dy)
                            correctTranslateXY()
                        }
                        invalidate()

                        mLastTouchX = x
                        mLastTouchY = y
                    } catch (e: Exception) {
                        val x = ev.x
                        val y = ev.y
                        if (!isZoom && mScaleFactor > 1 && mLastTouchX != INVALID_TOUCH_POSITION) {
                            val dx = x - mLastTouchX
                            val dy = y - mLastTouchY
                            setTranslateXY(mTranX + dx, mTranY + dy)
                            correctTranslateXY()
                        }
                        invalidate()

                        mLastTouchX = x
                        mLastTouchY = y
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mActivePointerId = MotionEvent.INVALID_POINTER_ID
                    mLastTouchX = INVALID_TOUCH_POSITION
                    mLastTouchY = INVALID_TOUCH_POSITION
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    val pointerIndex = ev.actionIndex
                    val pointerId = ev.getPointerId(pointerIndex)
                    if (pointerId == mActivePointerId) {
                        val newPointerIndex = if (pointerIndex == 0) 1 else 0
                        mLastTouchX = ev.getX(newPointerIndex)
                        mLastTouchY = ev.getY(newPointerIndex)
                        mActivePointerId = ev.getPointerId(newPointerIndex)
                    }
                }
            }
        }

        parent.requestDisallowInterceptTouchEvent(true)

        if (!mZooming && !mPinch)
            super.onTouchEvent(ev)

        return true
    }

    override fun canScrollVertically(direction: Int): Boolean {
        val scrolling = super.canScrollVertically(direction)

        if (!scrolling) {
            if (direction < 0 && mTranY >= 0)
                mSwipeOutListener?.onSwipeOutAtStart()
            else if (direction > 0 && mTranY <= mMaxTranY)
                mSwipeOutListener?.onSwipeOutAtEnd()
        }

        return scrolling
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(mTranX, mTranY)
        canvas.scale(mScaleFactor, mScaleFactor)
        super.dispatchDraw(canvas)
        canvas.restore()
    }

    override fun drawChild(canvas: Canvas, child: View?, drawingTime: Long): Boolean {
        val draw = super.drawChild(canvas, child, drawingTime)
        val lastItem = layoutManager?.childCount ?: -1
        if (isEnableZoom && mZooming && !mPinch && lastItem > 0 && child == getChildAt(lastItem -1)) {
            canvas.save()
            mPaint.shader = mShader
            mMagnifierMatrix.set(matrix)
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
            canvas.restore()
        }
        return draw
    }

    private fun setTranslateXY(tranX: Float, tranY: Float) {
        mTranX = tranX
        mTranY = tranY
    }

    private fun correctTranslateXY() {
        val correctXY = correctTranslateXY(mTranX, mTranY)
        mTranX = correctXY[0]
        mTranY = correctXY[1]
    }

    private fun correctTranslateXY(x: Float, y: Float): FloatArray {
        var x = x
        var y = y
        if (mScaleFactor <= 1) {
            return floatArrayOf(x, y)
        }
        if (x > 0.0f) {
            x = 0.0f
        } else if (x < mMaxTranX) {
            x = mMaxTranX
        }
        if (y > 0.0f) {
            y = 0.0f
        } else if (y < mMaxTranY) {
            y = mMaxTranY
        }
        return floatArrayOf(x, y)
    }

    private fun zoom(startVal: Float, endVal: Float) {
        if (mScaleAnimator == null)
            newZoomAnimation()

        if (mScaleAnimator!!.isRunning)
            return

        mMaxTranX = mViewWidth - mViewWidth * endVal
        mMaxTranY = mViewHeight - mViewHeight * endVal
        val startTranX = mTranX
        val startTranY = mTranY
        var endTranX = mTranX - (endVal - startVal) * mScaleCenterX
        var endTranY = mTranY - (endVal - startVal) * mScaleCenterY
        val correct = correctTranslateXY(endTranX, endTranY)
        endTranX = correct[0]
        endTranY = correct[1]
        val scaleHolder = PropertyValuesHolder.ofFloat(PROPERTY_SCALE, startVal, endVal)
        val tranXHolder = PropertyValuesHolder.ofFloat(PROPERTY_TRANX, startTranX, endTranX)
        val tranYHolder = PropertyValuesHolder.ofFloat(PROPERTY_TRANY, startTranY, endTranY)
        mScaleAnimator!!.setValues(scaleHolder, tranXHolder, tranYHolder)
        mScaleAnimator!!.duration = mScaleDuration.toLong()
        mScaleAnimator!!.start()
    }

    private fun newZoomAnimation() {
        mScaleAnimator = ValueAnimator()
        mScaleAnimator!!.interpolator = DecelerateInterpolator()
        mScaleAnimator!!.addUpdateListener { animation ->
            mScaleFactor = animation.getAnimatedValue(PROPERTY_SCALE) as Float

            setTranslateXY(
                animation.getAnimatedValue(PROPERTY_TRANX) as Float,
                animation.getAnimatedValue(PROPERTY_TRANY) as Float
            )
            invalidate()
        }

        mScaleAnimator!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                isZoom = true
            }

            override fun onAnimationEnd(animation: Animator) {
                isZoom = false
            }

            override fun onAnimationCancel(animation: Animator) {
                isZoom = false
            }
        })
    }

    private inner class ScaleListener : OnScaleGestureListener {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val mLastScale = mScaleFactor
            mScaleFactor *= detector.scaleFactor
            mScaleFactor = Math.max(mMinScaleFactor, Math.min(mScaleFactor, mMaxScaleFactor))
            mMaxTranX = mViewWidth - mViewWidth * mScaleFactor
            mMaxTranY = mViewHeight - mViewHeight * mScaleFactor
            mScaleCenterX = detector.focusX
            mScaleCenterY = detector.focusY
            val offsetX = mScaleCenterX * (mLastScale - mScaleFactor)
            val offsetY = mScaleCenterY * (mLastScale - mScaleFactor)
            setTranslateXY(mTranX + offsetX, mTranY + offsetY)
            isZoom = true
            invalidate()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            if (mScaleFactor <= mDefaultScaleFactor) {
                mScaleCenterX = -mTranX / (mScaleFactor - 1)
                mScaleCenterY = -mTranY / (mScaleFactor - 1)
                mScaleCenterX = if (mScaleCenterX.isNaN()) 0f else mScaleCenterX
                mScaleCenterY = if (mScaleCenterY.isNaN()) 0f else mScaleCenterY
                zoom(mScaleFactor, mDefaultScaleFactor)
            }
            isZoom = false
        }
    }

    private inner class GestureListener : SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val startFactor = mScaleFactor
            val endFactor: Float
            if (mScaleFactor == mDefaultScaleFactor) {
                mScaleCenterX = e.x
                mScaleCenterY = e.y
                endFactor = mMaxScaleFactor
            } else {
                mScaleCenterX = if (mScaleFactor == 1f) e.x else -mTranX / (mScaleFactor - 1)
                mScaleCenterY = if (mScaleFactor == 1f) e.y else -mTranY / (mScaleFactor - 1)
                endFactor = mDefaultScaleFactor
            }
            zoom(startFactor, endFactor)
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            return mZooming || super.onFling(e1, e2, velocityX, velocityY)
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            if (isEnableZoom) {
                mBitmap = this@ZoomRecyclerView.drawToBitmap()
                mShader = BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                mPaint = Paint()
                mZooming = true
                this@ZoomRecyclerView.invalidate()
            }
        }
    }

}