package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.widget.TextView
import br.com.fenix.bilingualreader.util.helpers.ImageUtil
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import br.com.fenix.bilingualreader.util.helpers.TextUtil
import org.slf4j.LoggerFactory

class ImageGetter(val context: Context, val textView: TextView, val maxWidth: Int = -1) : Html.ImageGetter {

    private val mLOGGER = LoggerFactory.getLogger(ImageGetter::class.java)

    override fun getDrawable(text: String): Drawable {
        var drawable =  BitmapDrawable(context.resources, "")
        try {
            val image = TextUtil.getImageFromTag(text)
            val base64 = image.substringAfter(",").trim()
            val screenWidth = if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                context.resources.displayMetrics.heightPixels
            else
                context.resources.displayMetrics.widthPixels
            val textViewWidth = textView.width - textView.paddingLeft - textView.paddingRight
            val targetWidth = when {
                maxWidth > 0 -> maxWidth
                textViewWidth > 0 -> textViewWidth
                else -> screenWidth
            }

            var bmp = if (maxWidth > 0 || textViewWidth > 0)
                ImageUtil.decodeImageBase64(base64, targetWidth, targetWidth)
            else
                ImageUtil.decodeImageBase64(base64)

            if (bmp != null) {
                if (bmp.width > targetWidth) {
                    val height = bmp.height * (targetWidth.toFloat() / bmp.width)
                    bmp = Bitmap.createScaledBitmap(bmp, targetWidth, height.toInt(), false)
                }

                drawable = BitmapDrawable(context.resources, bmp)
                drawable.mutate()
                drawable.setBounds(0, 0, bmp.width, bmp.height)
            }
        } catch (e: Exception) {
            mLOGGER.error("Error to load image: " + e.message, e)
            Telemetry.recordException(e, "Error to load image: " + e.message)
        }
        return drawable
    }
}
