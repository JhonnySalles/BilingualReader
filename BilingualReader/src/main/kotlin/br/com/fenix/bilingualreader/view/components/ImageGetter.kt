package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.content.res.Configuration
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
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            drawable = BitmapDrawable(context.resources, bmp)
            drawable.mutate();
            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                val width = if (bmp.width <= context.resources.displayMetrics.heightPixels) bmp.width else context.resources.displayMetrics.heightPixels
                val height = if (bmp.height <= context.resources.displayMetrics.widthPixels) bmp.height else context.resources.displayMetrics.widthPixels
                val start = if (isOnlyImage) (context.resources.displayMetrics.heightPixels / 2) - (width / 2) else 0
                drawable.setBounds(start, 0, start + width, height)
            } else {
                val width = if (bmp.width <= context.resources.displayMetrics.widthPixels) bmp.width else context.resources.displayMetrics.widthPixels
                val height = if (bmp.height <= context.resources.displayMetrics.heightPixels) bmp.height else context.resources.displayMetrics.heightPixels
                val start = if (isOnlyImage) (context.resources.displayMetrics.widthPixels / 2) - (width / 2) else 0
                drawable.setBounds(start, 0, start + width, height)
            }
        } catch (e: Exception) {
            mLOGGER.error("Error to load image", e)
        }
        return drawable
    }
}