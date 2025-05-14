package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView


class TextViewWithBorder : AppCompatTextView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    public override fun onDraw(canvas: Canvas) {
        val textColor = textColors.defaultColor
        setTextColor(Color.BLACK)
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        super.onDraw(canvas)
        setTextColor(textColor)
        paint.strokeWidth = 0f
        paint.style = Paint.Style.FILL
        super.onDraw(canvas)
    }
}