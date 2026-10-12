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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class ReadingHeatmapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val mEmptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val mSquareSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 13f, resources.displayMetrics)
    private val mSquareMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics)
    private val mCornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.5f, resources.displayMetrics)
    private val mDayLabelWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics)
    private val mMonthLabelHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18f, resources.displayMetrics)

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

        mTextPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 9f, resources.displayMetrics)
        mTextPaint.color = context.getColorFromAttr(R.attr.colorOnSurfaceVariant)
    }

    fun setData(data: Map<LocalDate, Int>) {
        mData = data
        mMaxPages = data.values.maxOrNull()?.coerceAtLeast(1) ?: 1
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        
        val availableWidth = (width - mDayLabelWidth).coerceAtLeast(0f)
        mCols = ((availableWidth + mSquareMargin) / (mSquareSize + mSquareMargin)).toInt().coerceAtLeast(1)
        
        val desiredHeight = mMonthLabelHeight + (mSquareSize * mRows) + (mSquareMargin * (mRows - 1))
        
        setMeasuredDimension(width, desiredHeight.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mCols == 0) return

        val today = LocalDate.now()
        
        val todayRow = today.dayOfWeek.value % 7 // 0=Sunday..6=Saturday
        val daysFromLastColEnd = 6 - todayRow
        val totalDaysToDraw = (mCols * mRows) - daysFromLastColEnd
        
        val startDate = today.minusDays(totalDaysToDraw.toLong() - 1)

        // Draw Day of Week labels on the left (Row 0 = Sun, Row 1 = Mon, ..., Row 6 = Sat)
        val locale = Locale.getDefault()
        for (row in 0 until mRows) {
            val top = mMonthLabelHeight + row * (mSquareSize + mSquareMargin)
            val centerY = top + mSquareSize / 2f
            val baselineY = centerY - (mTextPaint.descent() + mTextPaint.ascent()) / 2f
            val dayOfWeek = if (row == 0) DayOfWeek.SUNDAY else DayOfWeek.of(row)
            val dayInitial = dayOfWeek.getDisplayName(TextStyle.NARROW, locale).uppercase(locale)
            canvas.drawText(dayInitial, mDayLabelWidth / 2f, baselineY, mTextPaint)
        }

        // Draw Month labels on top centered above each month's columns
        data class MonthSpan(val name: String, var startCol: Int, var endCol: Int)
        val monthSpans = mutableListOf<MonthSpan>()

        for (col in 0 until mCols) {
            val date = startDate.plusDays((col * mRows).toLong())
            val monthName = date.month.getDisplayName(TextStyle.SHORT, locale)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                .replace(".", "")

            if (monthSpans.isEmpty() || monthSpans.last().name != monthName) {
                monthSpans.add(MonthSpan(monthName, col, col))
            } else {
                monthSpans.last().endCol = col
            }
        }

        for (span in monthSpans) {
            val startX = mDayLabelWidth + span.startCol * (mSquareSize + mSquareMargin)
            val endX = mDayLabelWidth + span.endCol * (mSquareSize + mSquareMargin) + mSquareSize
            val centerX = (startX + endX) / 2f
            val textWidth = mTextPaint.measureText(span.name)
            if ((endX - startX) >= textWidth * 0.7f) {
                val baselineY = (mMonthLabelHeight / 2f) - ((mTextPaint.descent() + mTextPaint.ascent()) / 2f)
                canvas.drawText(span.name, centerX, baselineY, mTextPaint)
            }
        }

        // Draw Heatmap squares
        val rect = RectF()
        
        for (col in 0 until mCols) {
            for (row in 0 until mRows) {
                val daysFromStart = (col * mRows) + row
                val date = startDate.plusDays(daysFromStart.toLong())

                if (date.isAfter(today)) continue

                val left = mDayLabelWidth + col * (mSquareSize + mSquareMargin)
                val top = mMonthLabelHeight + row * (mSquareSize + mSquareMargin)
                
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
