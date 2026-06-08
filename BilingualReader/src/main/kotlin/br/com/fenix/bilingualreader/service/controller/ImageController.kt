package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.ImageUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.URL

class ImageController private constructor() {

    companion object {
        val instance: ImageController by lazy { HOLDER.INSTANCE }
    }

    private val mLOGGER = LoggerFactory.getLogger(ImageController::class.java)

    private object HOLDER {
        val INSTANCE = ImageController()
    }

    private fun saveBitmapToCache(context: Context, key: String, bitmap: Bitmap) {
        try {
            val cacheDirBase = GeneralConsts.getCacheDir(context)
            val cacheDir = File(cacheDirBase, GeneralConsts.CACHE_FOLDER.IMAGE)
            if (!cacheDir.exists())
                cacheDir.mkdirs()

            val byte = ImageUtil.imageToByteArray(bitmap) ?: return
            val image = File(cacheDir, key)
            image.writeBytes(byte)
        } catch (e: Exception) {
            mLOGGER.error("Error save bitmap to cache: " + e.message, e)
        }
    }

    private fun getBitmapFromCache(context: Context, key: String): Bitmap? {
        try {
            val cacheDirBase = GeneralConsts.getCacheDir(context)
            val file = File(cacheDirBase, GeneralConsts.CACHE_FOLDER.IMAGE + '/' + key)

            if (file.exists())
                return BitmapFactory.decodeFile(file.absolutePath)

        } catch (e: Exception) {
            mLOGGER.error("Error retrieve bitmap from cache: " + e.message, e)
        }
        return null
    }

    private fun generateHash(link: String): String =
        Util.MD5(link)

    private fun getImage(context: Context, link: String): Bitmap? {
        if (link.isBlank() || link == "null")
            return null

        val hash = generateHash(link)
        var image: Bitmap? = getBitmapFromCache(context, hash)

        if (image == null) {
            try {
                val stream = URL(link).openStream()
                image = BitmapFactory.decodeStream(stream)
                if (image != null)
                    saveBitmapToCache(context, hash, image)
            } catch (e: IOException) {
                mLOGGER.error("Error retrieve bitmap from link: " + e.message, e)
            }
        }
        return image
    }

    // Internal dispatcher provider for testing
    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    internal var mainDispatcher: CoroutineDispatcher = Dispatchers.Main

    internal var imageScope: CoroutineScope? = null

    fun setImageAsync(context: Context, link: String, imageView: ImageView) {
        if (link.isBlank() || link == "null")
            return

        val scope = imageScope ?: CoroutineScope(mainDispatcher)
        scope.launch {
            try {
                val image = withContext(ioDispatcher) {
                    getImage(context, link)
                }
                
                if (image != null) {
                    imageView.setImageBitmap(image)
                    imageView.visibility = View.VISIBLE
                }
            } catch (m: OutOfMemoryError) {
                System.gc()
                mLOGGER.error("Memory full, cleaning", m)
            } catch (e: Exception) {
                mLOGGER.error("Error to get image async", e)
                Telemetry.recordException(e, "Error to get image async: " + e.message)
            }
        }
    }
}