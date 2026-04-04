package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class TextViewWithBorder : AppCompatTextView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val borderPaint = TextPaint()

    init {
        borderPaint.isAntiAlias = true
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 1f
        borderPaint.color = Color.BLACK
        borderPaint.strokeJoin = Paint.Join.ROUND
        borderPaint.strokeMiter = 10f
    }

    public override fun onDraw(canvas: Canvas) {
        val text = text
        if (text == null || text.isEmpty()) {
            super.onDraw(canvas)
            return
        }

        borderPaint.typeface = paint.typeface
        borderPaint.textSize = paint.textSize
        borderPaint.letterSpacing = paint.letterSpacing
        val x = compoundPaddingLeft.toFloat()
        val y = (height / 2f + borderPaint.textSize / 3f) + (totalPaddingTop - totalPaddingBottom) / 2f
        canvas.drawText(text.toString(), x, y, borderPaint)
        super.onDraw(canvas)
    }
}