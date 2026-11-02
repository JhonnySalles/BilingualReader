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
            val image = if (text.contains("<img")) TextUtil.getImageFromTag(text) else text
            val base64 = when {
                image.contains(";base64,") -> image.substringAfter(";base64,").trim()
                image.contains(",") -> image.substringAfter(",").trim()
                image.length > 100 -> image.trim()
                else -> null
            }
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

            var bmp = if (base64 != null) {
                if (maxWidth > 0 || textViewWidth > 0)
                    ImageUtil.decodeImageBase64(base64, targetWidth, targetWidth)
                else
                    ImageUtil.decodeImageBase64(base64)
            } else {
                val file = java.io.File(image)
                val candidateFile = if (file.exists()) file else {
                    br.com.ebook.foobnix.ext.CacheZipUtils.CACHE_BOOK_DIR?.listFiles()?.firstNotNullOfOrNull { dir ->
                        if (dir.isDirectory) {
                            val target = java.io.File(dir, image)
                            if (target.exists()) target else null
                        } else null
                    }
                }
                if (candidateFile != null && candidateFile.exists()) {
                    try {
                        android.graphics.BitmapFactory.decodeFile(candidateFile.absolutePath)
                    } catch (e: Exception) {
                        null
                    }
                } else null
            }

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
