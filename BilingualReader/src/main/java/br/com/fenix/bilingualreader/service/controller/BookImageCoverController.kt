package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.service.parses.book.ImageParse
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.ImageUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File


class BookImageCoverController private constructor() {

    companion object {
        val instance: BookImageCoverController by lazy { HOLDER.INSTANCE }
        val thread = newSingleThreadContext("BookCovers")
    }

    private val mLOGGER = LoggerFactory.getLogger(BookImageCoverController::class.java)

    private object HOLDER {
        val INSTANCE = BookImageCoverController()
    }

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 4
    private val lru = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    private fun saveBitmapToLru(key: String, bitmap: Bitmap) {
        try {
            synchronized(instance.lru) {
                if (instance.lru.get(key) == null)
                    instance.lru.put(key, bitmap)
            }
        } catch (e: Exception) {
            mLOGGER.warn("Error save image on LruCache: " + e.message, e)
        }
    }

    private fun retrieveBitmapFromLru(key: String): Bitmap? {
        try {
            return instance.lru.get(key)
        } catch (e: Exception) {
            mLOGGER.warn("Error retrieve image from LruCache: " + e.message, e)
        }
        return null
    }

    private fun saveBitmapToCache(context: Context, key: String, bitmap: Bitmap) {
        try {
            saveBitmapToLru(key, bitmap)
            val cacheDir =
                File(GeneralConsts.getCacheDir(context), GeneralConsts.CACHE_FOLDER.BOOK_COVERS)
            if (!cacheDir.exists())
                cacheDir.mkdir()

            val byte = ImageUtil.imageToByteArray(bitmap) ?: return
            val image = File(cacheDir.path + '/' + key)
            image.writeBytes(byte)
        } catch (e: Exception) {
            mLOGGER.error("Error save bitmap to cache: " + e.message, e)
        }
    }

    private fun retrieveBitmapFromCache(context: Context, key: String): Bitmap? {
        try {
            var image = retrieveBitmapFromLru(key)
            if (image != null) return image

            val file = File(
                GeneralConsts.getCacheDir(context),
                GeneralConsts.CACHE_FOLDER.BOOK_COVERS + '/' + key
            )

            if (file.exists()) {
                image = BitmapFactory.decodeFile(file.absolutePath)
                saveBitmapToLru(key, image)
                return image
            }
        } catch (e: Exception) {
            mLOGGER.error("Error retrieve bitmap from cache: " + e.message, e)
        }
        return null
    }

    fun saveCoverToCache(context: Context, book: Book, bitmap: Bitmap) {
        saveBitmapToCache(context, generateHash(book.file), bitmap)
    }

    fun getCoverFromFile(context: Context, file: File): Bitmap? {
        return getCoverFromFile(context, generateHash(file), file)
    }

    private fun generateHash(file: File): String =
        Util.MD5(file.path + file.name)

    private fun getCoverFromFile(
        context: Context,
        hash: String,
        file: File,
        isCoverSize: Boolean = true
    ): Bitmap? {
        val cover: Bitmap?

        if (isCoverSize) {
            cover = ImageParse(context).getCoverPage(file.path, true)

            if (cover != null)
                saveBitmapToCache(context, hash, cover)
        } else
            cover = ImageParse(context).getCoverPage(file.path, false)

        return cover
    }

    fun getBookCover(context: Context, book: Book, isCoverSize: Boolean): Bitmap? {
        val hash = generateHash(book.file)
        var image: Bitmap? = null

        if (isCoverSize)
            image = retrieveBitmapFromCache(context, hash)

        if (image == null) {
            if (!book.file.exists())
                return image

            image = getCoverFromFile(context, hash, book.file, isCoverSize)
        }

        return image
    }

    fun setImageCoverAsync(
        context: Context,
        book: Book,
        isCoverSize: Boolean = true,
        function: (Bitmap?) -> (Unit)
    ) {
        CoroutineScope(thread).launch {
            try {
                var image: Bitmap? = null
                val deferred = async {
                    image = getBookCover(context, book, isCoverSize)
                }
                deferred.await()
                withContext(Dispatchers.Main) {
                    function(image)
                }
            } catch (m: OutOfMemoryError) {
                System.gc()
                mLOGGER.error("Memory full, cleaning", m)
            } catch (e: Exception) {
                mLOGGER.error("Error to load image async", e)
            }
        }
    }

    fun setImageCoverAsync(context: Context, book: Book, imageView: ImageView, notLocate: Bitmap?, isCoverSize: Boolean = true) {
        setImageCoverAsync(context, book, isCoverSize) {
            val image = it ?: notLocate
            imageView.setImageBitmap(image)
        }
    }

    fun setImageCoverAsync(context: Context, book: Book, imagesView: ArrayList<ImageView>, notLocate: Bitmap?, isCoverSize: Boolean = true, onFinish: (Bitmap?) -> (Unit)) {
        setImageCoverAsync(context, book, isCoverSize) {
            val image = it ?: notLocate
            for (imageView in imagesView)
                imageView.setImageBitmap(image)

            onFinish(image)
        }
    }

    fun setImageCoverAsync(context: Context, book: Book, imageView: ImageView, notLocate: Bitmap?, isCoverSize: Boolean = true, onFinish: (Bitmap?) -> (Unit)) {
        setImageCoverAsync(context, book, isCoverSize) {
            val image = it ?: notLocate
            imageView.setImageBitmap(image)
            onFinish(image)
        }
    }

}