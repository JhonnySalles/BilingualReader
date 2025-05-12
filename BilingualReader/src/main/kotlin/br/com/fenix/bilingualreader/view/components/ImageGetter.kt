package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.widget.TextView
import br.com.fenix.bilingualreader.util.helpers.ImageUtil
import br.com.fenix.bilingualreader.util.helpers.TextUtil
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import org.slf4j.LoggerFactory


class ImageGetter(val context: Context, val textView: TextView) : Html.ImageGetter {

    private val mLOGGER = LoggerFactory.getLogger(ImageGetter::class.java)

    override fun getDrawable(text: String): Drawable {
        var drawable =  BitmapDrawable(context.resources, "")
        try {
            val image = TextUtil.getImageFromTag(text)
            var bmp = ImageUtil.decodeImageBase64(image.substringAfter(",").trim())

            if (bmp != null) {
                val screenWith = if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) context.resources.displayMetrics.heightPixels else context.resources.displayMetrics.widthPixels

                if (bmp.width > screenWith) {
                    val height = bmp.height * (screenWith.toFloat() / bmp.width)
                    bmp = Bitmap.createScaledBitmap(bmp, screenWith, height.toInt(), false)
                }

                drawable = BitmapDrawable(context.resources, bmp)
                drawable.mutate()
                drawable.setBounds(0, 0, bmp.width, bmp.height)
            }
        } catch (e: Exception) {
            mLOGGER.error("Error to load image: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error to load image: " + e.message)
                recordException(e)
            }
        }
        return drawable
    }
}