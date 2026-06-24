package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.core.graphics.withSave
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.interfaces.PageCurl
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import org.slf4j.LoggerFactory
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class PageCurlFrame @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr), PageCurl {

    private val mLOGGER = LoggerFactory.getLogger(PageCurlFrame::class.java)

    var isCurlPage = false
    var is3DMode = false
    private var mCurl = 0f
    private val mClipPath: Path = Path()
    private val mCurlPath: Path = Path()
    private val mCurlStrokePaint: Paint = Paint()
    private val mCurlFillPaint: Paint = Paint()
    private val mBottomFold = PointF()
    private val mTopFold = PointF()
    private val mBottomFoldTip = PointF()
    private val mTopFoldTip = PointF()

    private var mPageBitmap: Bitmap? = null
    private var mCanvasCache: Canvas? = null
    private val mShadowPaint = Paint().apply {
        isAntiAlias = true
    }

    private val mBackgroundPaint: Paint = Paint().apply {
        style = Paint.Style.FILL
        color = context.getColorFromAttr(android.R.attr.colorBackground)
    }

    private var mTouchY = -1f
    private val mMirrorMatrix = Matrix()
    private var mShadowGradient: Shader? = null
    private var mOverlayGradient: Shader? = null

    init {
        mCurlStrokePaint.style = Paint.Style.FILL
        mCurlStrokePaint.strokeWidth = 3.0f
        mCurlStrokePaint.color = Color.BLACK
        mCurlStrokePaint.maskFilter = BlurMaskFilter(150f, BlurMaskFilter.Blur.NORMAL)

        mCurlFillPaint.style = Paint.Style.FILL
        mCurlFillPaint.color = context.getColorFromAttr(R.attr.colorSurface)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        mTouchY = ev.y
        return super.dispatchTouchEvent(ev)
    }

    private fun updateBitmapCache() {
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return

        try {
            val currentBitmap = mPageBitmap
            if (currentBitmap == null || currentBitmap.width != w || currentBitmap.height != h) {
                mPageBitmap?.recycle()
                mPageBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                mCanvasCache = Canvas(mPageBitmap!!)
            }

            val canvas = mCanvasCache ?: return
            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            canvas.drawColor(mBackgroundPaint.color)
            
            val oldCurlPage = isCurlPage
            isCurlPage = false
            super.dispatchDraw(canvas)
            isCurlPage = oldCurlPage
        } catch (e: Exception) {
            mLOGGER.error("Failed to create page bitmap cache", e)
        }
    }

    override fun setCurlFactor(curl: Float) {
        val oldCurl = mCurl
        var factor = curl
        mCurl = curl
        val foldingPage = factor < 0

        if (isCurlPage && is3DMode) {
            if (mCurl != 0f && mCurl != 1f && mCurl != -1f) {
                if (mPageBitmap == null || oldCurl == 0f || oldCurl == 1f || oldCurl == -1f) {
                    updateBitmapCache()
                }
            } else {
                mPageBitmap?.recycle()
                mPageBitmap = null
                mCanvasCache = null
            }
        }

        val w = width.toFloat()
        val h = height.toFloat()

        if (factor < 0)
            factor += 1

        if (isCurlPage) {
            if (layerType != LAYER_TYPE_SOFTWARE) {
                setLayerType(LAYER_TYPE_SOFTWARE, null)
            }
        } else {
            if (layerType != LAYER_TYPE_NONE) {
                setLayerType(LAYER_TYPE_NONE, null)
            }
        }

        if (is3DMode) {
            val touchY = if (mTouchY >= 0f) mTouchY.coerceIn(0f, h) else h
            val maxAngle = Math.toRadians(25.0).toFloat()
            val angleTaper = sin(factor * Math.PI.toFloat())
            val alpha = ((touchY / h) - 0.5f) * maxAngle * angleTaper

            val x0 = w * factor
            val tanAlpha = kotlin.math.tan(alpha)
            val xTop = x0 + tanAlpha * touchY
            val xBot = x0 - tanAlpha * (h - touchY)

            mBottomFold.x = xBot
            mBottomFold.y = h
            mTopFold.x = xTop
            mTopFold.y = 0f

            val dx = xBot - xTop
            val dy = h
            val angleRad = atan2(dy.toDouble(), dx.toDouble()).toFloat()
            val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()

            mMirrorMatrix.reset()
            mMirrorMatrix.postTranslate(-xTop, 0f)
            mMirrorMatrix.postRotate(-angleDeg)
            mMirrorMatrix.postScale(1f, -1f)
            mMirrorMatrix.postRotate(angleDeg)
            mMirrorMatrix.postTranslate(xTop, 0f)

            mClipPath.reset()
            mClipPath.moveTo(0f, 0f)
            mClipPath.lineTo(xTop, 0f)
            mClipPath.lineTo(xBot, h)
            mClipPath.lineTo(0f, h)
            mClipPath.close()

            val rightPath = Path().apply {
                moveTo(xTop, 0f)
                lineTo(w * 2f, 0f)
                lineTo(w * 2f, h)
                lineTo(xBot, h)
                close()
            }
            mCurlPath.set(rightPath)
            mCurlPath.transform(mMirrorMatrix)

            val angleNormal = angleRad - Math.PI.toFloat() / 2f
            val normX = cos(angleNormal)
            val normY = sin(angleNormal)

            val shadowWidth = (w * 0.15f).coerceIn(50f, 250f)
            val cx = x0
            val cy = touchY

            val startX = cx - normX * shadowWidth
            val startY = cy - normY * shadowWidth
            val endX = cx + normX * shadowWidth
            val endY = cy + normY * shadowWidth

            mShadowGradient = LinearGradient(
                startX, startY, endX, endY,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(120, 0, 0, 0),
                    Color.argb(180, 0, 0, 0),
                    Color.argb(40, 255, 255, 255), // Softer highlight
                    Color.argb(40, 0, 0, 0),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0.0f, 0.48f, 0.5f, 0.54f, 0.65f, 1.0f),
                Shader.TileMode.CLAMP
            )

            // Paper color gradient overlay that fades away from the crease (to the left)
            val surfaceColor = context.getColorFromAttr(R.attr.colorSurface)
            val r = Color.red(surfaceColor)
            val g = Color.green(surfaceColor)
            val b = Color.blue(surfaceColor)
            val fadeLength = (w - x0).coerceAtLeast(50f) * 0.5f

            mOverlayGradient = LinearGradient(
                cx, cy,
                cx - normX * fadeLength, cy - normY * fadeLength,
                intArrayOf(
                    Color.argb(160, r, g, b),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0.0f, 1.0f),
                Shader.TileMode.CLAMP
            )
        } else {
            mBottomFold.x = w * factor
            mBottomFold.y = h

            if (mBottomFold.x > w / 2) {
                mTopFold.x = w
                mTopFold.y = h - (w - mBottomFold.x) * h / mBottomFold.x
            } else {
                mTopFold.x = 2 * mBottomFold.x
                mTopFold.y = 0f
            }

            val angle = atan((h - mTopFold.y) / (mTopFold.x - mBottomFold.x))

            val cosFactor = cos(2 * angle)
            val sinFactor = sin(2 * angle)

            val foldWidth: Float = w - mBottomFold.x
            mBottomFoldTip.x = mBottomFold.x + foldWidth * cosFactor
            mBottomFoldTip.y = (h - foldWidth * sinFactor).toFloat()

            if (mBottomFold.x > w / 2) {
                mTopFoldTip.x = mTopFold.x
                mTopFoldTip.y = mTopFold.y
            } else {
                mTopFoldTip.x = (mTopFold.x + (w - mTopFold.x) * cosFactor)
                mTopFoldTip.y = -(sinFactor * (w - mTopFold.x))
            }

            mClipPath.reset()
            if (foldingPage) {
                mClipPath.moveTo(0f, 0f)
                if (mTopFold.y != 0f)
                    mClipPath.lineTo(w, 0f)

                mClipPath.lineTo(mTopFold.x, mTopFold.y)
                mClipPath.lineTo(mBottomFold.x, mBottomFold.y)
                mClipPath.lineTo(0f, h)
            } else {
                mClipPath.moveTo(w, h)
                if (mTopFold.y == 0f)
                    mClipPath.lineTo(w, 0f)

                mClipPath.lineTo(mTopFold.x, mTopFold.y)
                mClipPath.lineTo(mBottomFold.x, mBottomFold.y)
            }
            mClipPath.close()

            mCurlPath.reset()
            mCurlPath.moveTo(mBottomFold.x, mBottomFold.y)
            mCurlPath.lineTo(mBottomFoldTip.x, mBottomFoldTip.y)
            mCurlPath.lineTo(mTopFoldTip.x, mTopFoldTip.y)
            mCurlPath.lineTo(mTopFold.x, mTopFold.y)
            mCurlPath.close()
        }

        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (isCurlPage) {
            canvas.withSave() {
                if (mCurl != 0f && mCurl != 1f && mCurl != -1f)
                    clipPath(mClipPath)

                drawRect(0f, 0f, width.toFloat(), height.toFloat(), mBackgroundPaint)
                super.dispatchDraw(canvas)
            }

            if (mCurl < 0f) {
                if (is3DMode) {
                    val bitmap = mPageBitmap
                    if (bitmap != null) {
                        canvas.withSave {
                            clipPath(mCurlPath)
                            drawBitmap(bitmap, mMirrorMatrix, null)

                            val gradOverlay = mOverlayGradient
                            if (gradOverlay != null) {
                                val overlayPaint = Paint().apply {
                                    shader = gradOverlay
                                    style = Paint.Style.FILL
                                }
                                drawPath(mCurlPath, overlayPaint)
                            }
                        }
                    } else {
                        canvas.drawPath(mCurlPath, mCurlFillPaint)
                    }

                    val grad = mShadowGradient
                    if (grad != null) {
                        mShadowPaint.shader = grad
                        canvas.withSave {
                            clipPath(mClipPath)
                            drawRect(0f, 0f, width.toFloat(), height.toFloat(), mShadowPaint)
                        }
                        canvas.withSave {
                            clipPath(mCurlPath)
                            drawRect(0f, 0f, width.toFloat(), height.toFloat(), mShadowPaint)
                        }
                    }
                } else {
                    canvas.drawPath(mCurlPath, mCurlStrokePaint)
                    canvas.drawPath(mCurlPath, mCurlFillPaint)
                }
            }
        } else {
            super.dispatchDraw(canvas)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mPageBitmap?.recycle()
        mPageBitmap = null
        mCanvasCache = null
    }
}