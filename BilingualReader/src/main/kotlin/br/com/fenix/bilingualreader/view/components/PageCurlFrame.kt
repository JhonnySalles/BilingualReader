package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.graphics.withSave
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.interfaces.PageCurl
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import org.slf4j.LoggerFactory
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

class PageCurlFrame @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr), PageCurl {

    private val mLOGGER = LoggerFactory.getLogger(PageCurlFrame::class.java)

    var isCurlPage = false
    private var mCurl = 0f
    private val mClipPath: Path = Path()
    private val mCurlPath: Path = Path()
    private val mCurlStrokePaint: Paint = Paint()
    private val mCurlFillPaint: Paint = Paint()
    private val mBottomFold = PointF()
    private val mTopFold = PointF()
    private val mBottomFoldTip = PointF()
    private val mTopFoldTip = PointF()

    init {
        mCurlStrokePaint.style = Paint.Style.FILL
        mCurlStrokePaint.strokeWidth = 3.0f
        mCurlStrokePaint.color = Color.BLACK
        mCurlStrokePaint.maskFilter = BlurMaskFilter(150f, BlurMaskFilter.Blur.NORMAL)

        mCurlFillPaint.style = Paint.Style.FILL
        mCurlFillPaint.color = context.getColorFromAttr(R.attr.colorSurface)
    }

    override fun setCurlFactor(curl: Float) {
        var factor = curl
        mCurl = curl
        val foldingPage = factor < 0

        val w = width.toFloat()
        val h = height.toFloat()

        if (factor < 0)
            factor += 1

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

        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (isCurlPage) {
            canvas.withSave() {
                if (mCurl != 0f && mCurl != 1f && mCurl != -1f)
                    clipPath(mClipPath)

                super.dispatchDraw(canvas)
            }

            if (mCurl < 0f) {
                canvas.drawPath(mCurlPath, mCurlStrokePaint)
                canvas.drawPath(mCurlPath, mCurlFillPaint)
            }
        } else
            super.dispatchDraw(canvas)
    }

}