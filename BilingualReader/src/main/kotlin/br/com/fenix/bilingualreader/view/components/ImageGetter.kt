package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.util.Base64
import android.widget.TextView
import org.slf4j.LoggerFactory


class ImageGetter(val context: Context, val textView: TextView, val isOnlyImage : Boolean = true) : Html.ImageGetter {

    private val mLOGGER = LoggerFactory.getLogger(ImageGetter::class.java)

    override fun getDrawable(text: String): Drawable {
        var drawable =  BitmapDrawable(context.resources, "")
        try {
            val image = text.substringAfter("<img").substringBefore("/>")
            val bytes: ByteArray = Base64.decode(image.substringAfter(",").trim(), Base64.DEFAULT)
            var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            val screenWith = if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) context.resources.displayMetrics.heightPixels else context.resources.displayMetrics.widthPixels

            if (bmp.width > screenWith) {
                val height = bmp.height * (screenWith.toFloat() / bmp.width)
                bmp = Bitmap.createScaledBitmap(bmp, screenWith, height.toInt(), false)
            }

            drawable = BitmapDrawable(context.resources, bmp)
            drawable.mutate()

            val start = (if (isOnlyImage) (screenWith / 2f) - (bmp.width / 2f) else 0).toInt()
            drawable.setBounds(start, 0, start + bmp.width, bmp.height)
        } catch (e: Exception) {
            mLOGGER.error("Error to load image", e)
        }
        return drawable
    }
}