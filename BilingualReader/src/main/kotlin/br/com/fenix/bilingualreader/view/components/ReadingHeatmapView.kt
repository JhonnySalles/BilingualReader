package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import java.time.LocalDate

class ReadingHeatmapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val mEmptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val mSquareSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 13f, resources.displayMetrics)
    private val mSquareMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics)
    private val mCornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.5f, resources.displayMetrics)

    private val mBaseColor: Int
    private val mEmptyColor: Int

    private var mData: Map<LocalDate, Int> = emptyMap()
    private var mMaxPages: Int = 1

    private var mCols = 0
    private val mRows = 7

    init {
        mBaseColor = context.getColorFromAttr(androidx.appcompat.R.attr.colorPrimary)
        mEmptyColor = context.getColorFromAttr(R.attr.colorSurfaceVariant)
        mEmptyPaint.color = mEmptyColor
    }

    fun setData(data: Map<LocalDate, Int>) {
        mData = data
        mMaxPages = data.values.maxOrNull()?.coerceAtLeast(1) ?: 1
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        
        mCols = ((width + mSquareMargin) / (mSquareSize + mSquareMargin)).toInt()
        
        val desiredHeight = (mSquareSize * mRows) + (mSquareMargin * (mRows - 1))
        
        setMeasuredDimension(width, desiredHeight.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mCols == 0) return

        val today = LocalDate.now()
        
        // Em um mapa do github, a coluna mais a direita é a semana atual.
        // O último quadradinho do canto inferior direito seria hoje, ou o dia de hoje.
        val todayRow = today.dayOfWeek.value % 7 // 0=Sunday..6=Saturday if we use standard android week, but LocalDate is 1=Monday..7=Sunday. Let's use 0=Monday..6=Sunday.
        val daysFromLastColEnd = 6 - (today.dayOfWeek.value - 1)
        val totalDaysToDraw = (mCols * mRows) - daysFromLastColEnd
        
        val startDate = today.minusDays(totalDaysToDraw.toLong() - 1)

        val rect = RectF()
        
        for (col in 0 until mCols) {
            for (row in 0 until mRows) {
                val daysFromStart = (col * mRows) + row
                val date = startDate.plusDays(daysFromStart.toLong())

                if (date.isAfter(today)) continue

                val left = col * (mSquareSize + mSquareMargin)
                val top = row * (mSquareSize + mSquareMargin)
                
                rect.set(left, top, left + mSquareSize, top + mSquareSize)

                val pages = mData[date] ?: 0
                if (pages == 0) {
                    canvas.drawRoundRect(rect, mCornerRadius, mCornerRadius, mEmptyPaint)
                } else {
                    val intensity = (pages.toFloat() / mMaxPages.coerceAtMost(100)).coerceIn(0.2f, 1.0f)
                    mPaint.color = adjustAlpha(mBaseColor, intensity)
                    canvas.drawRoundRect(rect, mCornerRadius, mCornerRadius, mPaint)
                }
            }
        }
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(Color.alpha(color) * factor)
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
