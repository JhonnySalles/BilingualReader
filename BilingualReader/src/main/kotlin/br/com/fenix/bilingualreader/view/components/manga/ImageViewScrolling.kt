package br.com.fenix.bilingualreader.view.components.manga

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.ViewCompat
import androidx.core.view.drawToBitmap
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.interfaces.BaseImageView
import br.com.fenix.bilingualreader.view.components.manga.ImageViewPage
import org.slf4j.LoggerFactory


open class ImageViewScrolling(context: Context, attributeSet: AttributeSet?) : AppCompatImageView(context, attributeSet), BaseImageView  {

    constructor(context: Context) : this(context, null)

    private val mLOGGER = LoggerFactory.getLogger(ImageViewScrolling::class.java)

    private val m = FloatArray(9)

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun getPointerCoordinate(e: MotionEvent): FloatArray {
        val index = e.actionIndex
        val coordinates = floatArrayOf(e.getX(index), e.getY(index))
        val matrix = Matrix()
        imageMatrix.invert(matrix)
        matrix.mapPoints(coordinates)
        val imageSize = computeCurrentImageSize()
        return floatArrayOf(
            coordinates[0],
            coordinates[1],
            imageSize.x.toFloat(),
            imageSize.y.toFloat()
        )
    }

    open fun computeCurrentImageSize(): Point {
        val size = Point()
        val d: Drawable = drawable ?: return size
        imageMatrix.getValues(m)
        val scale = m[Matrix.MSCALE_X]
        val width = d.intrinsicWidth * scale
        val height = d.intrinsicHeight * scale
        size[width.toInt()] = height.toInt()
        return size
    }

    override val isApplyPercent: Boolean = false
    override fun getScrollPercent(): Triple<Float, Float, Float> = Triple(0F, 0F, 0F)
    override fun setScrollPercent(percent: Triple<Float, Float, Float>) { }

}