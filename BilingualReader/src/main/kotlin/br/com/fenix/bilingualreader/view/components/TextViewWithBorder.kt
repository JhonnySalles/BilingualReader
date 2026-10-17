package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * Text with a dark outline drawn via [android.graphics.Paint.setShadowLayer] in a
 * single [onDraw] pass. Prefer this over a double [super.onDraw] stroke/fill so
 * BlurView's software root capture does not pay for the text twice.
 */
class TextViewWithBorder : AppCompatTextView {
    constructor(context: Context) : super(context) {
        applyBorder()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        applyBorder()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        applyBorder()
    }

    private fun applyBorder() {
        paint.setShadowLayer(1.5f, 0f, 0f, Color.BLACK)
    }

    public override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
    }
}
