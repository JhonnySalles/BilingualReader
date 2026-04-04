package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class TextViewWithBorder : AppCompatTextView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    public override fun onDraw(canvas: Canvas) {
        val originalStyle = paint.style
        val originalStrokeWidth = paint.strokeWidth
        val originalColorFilter = paint.colorFilter

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeMiter = 10f
        paint.colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
        super.onDraw(canvas)

        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.colorFilter = originalColorFilter
        super.onDraw(canvas)

        paint.style = originalStyle
        paint.strokeWidth = originalStrokeWidth
    }
}