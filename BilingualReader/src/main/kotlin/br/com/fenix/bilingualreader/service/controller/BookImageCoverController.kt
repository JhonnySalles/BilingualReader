package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.service.parses.book.ImageParse
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.ImageUtil
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import br.com.fenix.bilingualreader.util.helpers.Util
import coil.load
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException


class BookImageCoverController private constructor() {

    companion object {
        val instance: BookImageCoverController by lazy { HOLDER.INSTANCE }
        val thread: CoroutineDispatcher = java.util.concurrent.Executors.newFixedThreadPool(3).asCoroutineDispatcher()
    }

    private val mLOGGER = LoggerFactory.getLogger(BookImageCoverController::class.java)

    private object HOLDER {
        val INSTANCE = BookImageCoverController()
    }

    private fun saveBitmapToCache(context: Context, key: String, bitmap: Bitmap) {
        try {
            val cacheDir = File(GeneralConsts.getCoverDir(context), GeneralConsts.CACHE_FOLDER.BOOK_COVERS)
            if (!cacheDir.exists())
                cacheDir.mkdir()

            val byte = ImageUtil.imageToByteArray(bitmap) ?: return
            val image = File(cacheDir.path + '/' + key)
            image.writeBytes(byte)
        } catch (e: Exception) {
            mLOGGER.error("Error save bitmap to cache: " + e.message, e)
            Telemetry.recordException(e, "Error save bitmap to cache: " + e.message)
        }
    }

    private fun retrieveBitmapFromCache(context: Context, key: String): Bitmap? {
        try {
            val file = File(GeneralConsts.getCoverDir(context), GeneralConsts.CACHE_FOLDER.BOOK_COVERS + '/' + key)

            if (file.exists()) {
                return ImageUtil.decodeFile(file) ?: return null
            }
        } catch (e: Exception) {
            mLOGGER.error("Error retrieve bitmap from cache: " + e.message, e)
            Telemetry.recordException(e, "Error retrieve bitmap from cache: " + e.message)
        }
        return null
    }

    fun saveCoverToCache(context: Context, book: Book, bitmap: Bitmap) {
        saveBitmapToCache(context, generateHash(book.file), bitmap)
    }

    fun getCoverFromFile(context: Context, file: File): Bitmap? {
        return getCoverFromFile(context, generateHash(file), file)
    }

    private fun generateHash(file: File): String = Util.MD5(file.path + file.name)

    private fun getCoverFromFile(context: Context, hash: String, file: File, isCoverSize: Boolean = true): Bitmap? {
        val cover: Bitmap?

        if (isCoverSize) {
            cover = ImageParse(context).getCoverPage(file.path, true)

            if (cover != null)
                saveBitmapToCache(context, hash, cover)
        } else
            cover = ImageParse(context).getCoverPage(file.path, false)

        return cover
    }

    fun getBookCoverFile(context: Context, book: Book, isCoverSize: Boolean): File? {
        val hash = generateHash(book.file)
        val cacheDir = File(GeneralConsts.getCoverDir(context), GeneralConsts.CACHE_FOLDER.BOOK_COVERS)
        val cacheFile = File(cacheDir, hash)

        if (isCoverSize && cacheFile.exists()) {
            return cacheFile
        }

        val file = book.file
        if (!file.exists()) {
            return null
        }

        val cover = getCoverFromFile(context, hash, file, isCoverSize)
        if (cover != null) {
            return cacheFile
        }

        return null
    }

    fun getBookCover(context: Context, book: Book, isCoverSize: Boolean): Bitmap? {
        val hash = generateHash(book.file)
        var image: Bitmap? = null

        if (isCoverSize)
            image = retrieveBitmapFromCache(context, hash)

        if (image == null) {
            val file = book.file
            if (!file.exists())
                return image

            image = getCoverFromFile(context, hash, book.file, isCoverSize)
        }

        return image
    }

    fun setImageCoverAsync(context: Context, book: Book, isCoverSize: Boolean = true, function: (Bitmap?) -> (Unit)) {
        CoroutineScope(thread).launch {
            try {
                val image: Bitmap? = getBookCover(context, book, isCoverSize)
                withContext(Dispatchers.Main) {
                    function(image)
                }
            } catch (m: OutOfMemoryError) {
                System.gc()
                mLOGGER.error("Memory full, cleaning", m)
            } catch (m: IOException) {
                mLOGGER.error("Error to load image async: " + book.name, m)
                Telemetry.recordException(m, "Error to load image async: " + m.message)
            } catch (e: FileNotFoundException) {
                mLOGGER.error("File not found. Error to load image async: " + book.name, e)
            } catch (e: Exception) {
                mLOGGER.error("Error to load image async: " + e.message, e)
                Telemetry.recordException(e, "Error to load image async: " + e.message)
            }
        }
    }

    fun setImageCoverAsync(context: Context, book: Book, imageView: ImageView, notLocate: Bitmap?, @Suppress("UNUSED_PARAMETER") isCoverSize: Boolean = true) {
        imageView.load(book) {
            allowHardware(false)
            crossfade(true)
            if (notLocate != null) {
                placeholder(BitmapDrawable(context.resources, notLocate))
                error(BitmapDrawable(context.resources, notLocate))
            }
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

    fun setImageCoverAsync(context: Context, book: Book, imageView: ImageView, notLocate: Bitmap?, @Suppress("UNUSED_PARAMETER") isCoverSize: Boolean = true, onFinish: (Bitmap?) -> (Unit)) {
        imageView.load(book) {
            allowHardware(false)
            crossfade(true)
            if (notLocate != null) {
                placeholder(BitmapDrawable(context.resources, notLocate))
                error(BitmapDrawable(context.resources, notLocate))
            }
            target(
                onSuccess = { result ->
                    val bitmap = (result as? BitmapDrawable)?.bitmap
                    imageView.setImageBitmap(bitmap)
                    onFinish(bitmap)
                },
                onError = {
                    imageView.setImageBitmap(notLocate)
                    onFinish(notLocate)
                }
            )
        }
    }

}