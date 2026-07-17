package br.com.ebook.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout.Alignment
import android.text.StaticLayout
import android.text.TextPaint
import br.com.ebook.foobnix.android.utils.Dips
import br.com.ebook.foobnix.android.utils.TxtUtils
import br.com.ebook.foobnix.pdf.info.IMG
import br.com.ebook.foobnix.pdf.info.TintUtil
import br.com.ebook.foobnix.pdf.info.wrapper.AppState
import br.com.ebook.foobnix.pdf.info.wrapper.MagicHelper
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

object BitmapUtils {
    private val LOGGER = LoggerFactory.getLogger(BitmapUtils::class.java)
    private const val BUFFER_SIZE = 16 * 1024

    @JvmStatic
    fun getBookCoverWithTitleBitmap(title: String?, author: String?): Bitmap {
        val cleanAuthor = if (TxtUtils.isEmpty(author)) "" else author!!
        var cleanTitle = if (TxtUtils.isEmpty(title)) "" else title!!

        cleanTitle = TxtUtils.ellipsize(cleanTitle, 20)
        val shortAuthor = TxtUtils.ellipsize(cleanAuthor, 40)

        val w = Dips.dpToPx(AppState.get().coverBigSize - 8)
        val h = (w * IMG.WIDTH_DK).toInt()

        val pNormal = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (h / 11).toFloat()
        }

        val pBold = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (h / 14).toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)
        c.save()

        val colors = intArrayOf(Color.BLACK, TintUtil.randomColor((cleanTitle + shortAuthor).hashCode()), Color.BLACK)
        val positions = floatArrayOf(0.1f, 0.5f, 0.9f)
        val gradient = LinearGradient(0f, h.toFloat(), w.toFloat(), 0f, colors, positions, Shader.TileMode.CLAMP)
        val p = Paint().apply {
            isDither = true
            shader = gradient
        }
        c.drawPaint(p)

        val margin = Dips.dpToPx(10)
        val mTextLayout = StaticLayout(cleanTitle, pBold, c.width - margin * 2, Alignment.ALIGN_CENTER, 1.0f, 0.0f, false)
        c.translate(margin.toFloat(), Dips.dpToPx(20).toFloat())
        mTextLayout.draw(c)

        val text2 = StaticLayout(shortAuthor, pNormal, c.width - margin * 2, Alignment.ALIGN_CENTER, 1.0f, 0.0f, false)
        c.translate(0f, (mTextLayout.height + margin).toFloat())
        text2.draw(c)
        return bitmap
    }

    @JvmStatic
    fun getBookCoverWithTitle(title: String?, author: String?, withLogo: Boolean): Bitmap? {
        return try {
            val bookCoverWithTitleBitmap = getBookCoverWithTitleBitmap(title, author)
            if (withLogo) {
                MagicHelper.applyBookEffectWithLogo(bookCoverWithTitleBitmap)
            }
            bookCoverWithTitleBitmap
        } catch (e: OutOfMemoryError) {
            LOGGER.error("Error to get book cover: {}", e.message, e)
            null
        }
    }

    @JvmStatic
    fun arrayToBitmap(array: ByteArray, width: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(array, 0, array.size, options)
            val imageWidth = options.outWidth
            options.inSampleSize = imageWidth / width
            options.inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(array, 0, array.size, options)
        } catch (e: Exception) {
            LOGGER.error("Error array to bitmap: {}", e.message, e)
            null
        }
    }

    @JvmStatic
    fun decodeImage(path: String, width: Int): InputStream? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)
            val imageWidth = options.outWidth
            options.inSampleSize = imageWidth / width
            options.inJustDecodeBounds = false
            val decodeFile = BitmapFactory.decodeFile(path, options)

            ByteArrayOutputStream().use { stream ->
                decodeFile.compress(Bitmap.CompressFormat.PNG, 95, stream)
                ByteArrayInputStream(stream.toByteArray())
            }
        } catch (e: Exception) {
            LOGGER.error("Error decode image: {}", e.message, e)
            null
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun getEntryAsByte(zipInputStream: InputStream): ByteArray {
        ByteArrayOutputStream().use { out ->
            val bytesIn = ByteArray(BUFFER_SIZE)
            var read: Int
            while (zipInputStream.read(bytesIn).also { read = it } != -1) {
                out.write(bytesIn, 0, read)
            }
            return out.toByteArray()
        }
    }
}
