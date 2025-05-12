package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.service.parses.manga.RarParse
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.ImageUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream


class MangaImageCoverController private constructor() {

    companion object {
        val instance: MangaImageCoverController by lazy { HOLDER.INSTANCE }
        val thread = newSingleThreadContext("MangaCovers")
    }

    private val mLOGGER = LoggerFactory.getLogger(MangaImageCoverController::class.java)

    private object HOLDER {
        val INSTANCE = MangaImageCoverController()
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
                    instance.lru.remove(key)
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
            val cacheDir = File(GeneralConsts.getCoverDir(context), GeneralConsts.CACHE_FOLDER.MANGA_COVERS)
            if (!cacheDir.exists())
                cacheDir.mkdir()

            val byte = ImageUtil.imageToByteArray(bitmap) ?: return
            val image = File(cacheDir.path + '/' + key)
            image.writeBytes(byte)
        } catch (e: Exception) {
            mLOGGER.error("Error save bitmap to cache: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error save bitmap to cache: " + e.message)
                recordException(e)
            }
        }
    }

    private fun retrieveBitmapFromCache(context: Context, key: String): Bitmap? {
        try {
            var image = retrieveBitmapFromLru(key)
            if (image != null) return image

            val file = File(GeneralConsts.getCoverDir(context), GeneralConsts.CACHE_FOLDER.MANGA_COVERS + '/' + key)

            if (file.exists()) {
                image = BitmapFactory.decodeFile(file.absolutePath) ?: return null
                saveBitmapToLru(key, image)
                return image
            }
        } catch (e: Exception) {
            mLOGGER.error("Error retrieve bitmap from cache: " + e.message, e)
            Firebase.crashlytics.apply {
                setCustomKey("message", "Error retrieve bitmap from cache: " + e.message)
                recordException(e)
            }
        }
        return null
    }

    fun saveCoverToCache(context: Context, manga: Manga, bitmap: Bitmap) {
        saveBitmapToCache(context, generateHash(manga.file), bitmap)
    }

    fun getCoverFromFile(context: Context, file: File, parse: Parse): Bitmap? {
        return getCoverFromFile(context, generateHash(file), parse)
    }

    private fun generateHash(file: File): String = Util.MD5(file.path + file.name)

    private fun getCoverFromFile(context: Context, hash: String, parse: Parse, isCoverSize: Boolean = true): Bitmap? {
        var index = 0
        for (i in 0 until parse.numPages()) {
            if (FileUtil.isImage(parse.getPagePath(i)!!)) {
                index = i
                break
            }
        }
        var stream: InputStream? = parse.getPage(index)

        val cover: Bitmap?

        if (isCoverSize) {
            val option = BitmapFactory.Options()
            option.inJustDecodeBounds = true
            BitmapFactory.decodeStream(stream, null, option)
            option.inSampleSize = ImageUtil.calculateInSampleSize(
                option,
                ReaderConsts.COVER.MANGA_COVER_THUMBNAIL_WIDTH,
                ReaderConsts.COVER.MANGA_COVER_THUMBNAIL_HEIGHT
            )
            option.inJustDecodeBounds = false

            Util.closeInputStream(stream)
            stream = parse.getPage(index)
            cover = BitmapFactory.decodeStream(stream, null, option)
            if (cover != null)
                saveBitmapToCache(context, hash, cover)

            Util.closeInputStream(stream)
        } else {
            stream = parse.getPage(index)
            cover = BitmapFactory.decodeStream(stream)
            Util.closeInputStream(stream)
        }

        return cover
    }

    fun getMangaCover(context: Context, manga: Manga, isCoverSize: Boolean): Bitmap? {
        val hash = generateHash(manga.file)
        var image: Bitmap? = null

        if (isCoverSize)
            image = retrieveBitmapFromCache(context, hash)

        if (image == null) {
            if (!manga.file.exists())
                return image

            val parse = ParseFactory.create(manga.file) ?: return image
            try {
                if (parse is RarParse) {
                    val folder = GeneralConsts.CACHE_FOLDER.RAR + '/' + Util.normalizeNameCache(manga.file.nameWithoutExtension)
                    val cacheDir = File(GeneralConsts.getCacheDir(context), folder)
                    (parse as RarParse?)!!.setCacheDirectory(cacheDir)
                }

                image = getCoverFromFile(context, hash, parse, isCoverSize)
            } finally {
                Util.destroyParse(parse)
            }
        }

        return image
    }

    fun setImageCoverAsync(context: Context, manga: Manga, isCoverSize: Boolean = true, function: (Bitmap?) -> (Unit)) {
        CoroutineScope(thread).launch {
            try {
                async {
                    val image: Bitmap? = getMangaCover(context, manga, isCoverSize)
                    withContext(Dispatchers.Main) {
                        function(image)
                    }
                }
            } catch (m: OutOfMemoryError) {
                System.gc()
                mLOGGER.error("Memory full, cleaning", m)
            } catch (m: IOException) {
                mLOGGER.error("Error to load image async: " + manga.name, m)
                Firebase.crashlytics.apply {
                    setCustomKey("message", "Error to load image async: " + m.message)
                    recordException(m)
                }
            } catch (e: FileNotFoundException) {
                mLOGGER.error("File not found. Error to load image async: " + manga.name, e)
            } catch (e: Exception) {
                mLOGGER.error("Error to load image async: " + manga.name, e)
                Firebase.crashlytics.apply {
                    setCustomKey("message", "Error to load image async: " + e.message)
                    recordException(e)
                }
            }
        }
    }

    fun setImageCoverAsync(context: Context, manga: Manga, imageView: ImageView, notLocate: Bitmap?, isCoverSize: Boolean = true) {
        setImageCoverAsync(context, manga, isCoverSize) {
            val image = it ?: notLocate
            imageView.setImageBitmap(image)
        }
    }

    fun setImageCoverAsync(context: Context, manga: Manga, imagesView: ArrayList<ImageView>, notLocate: Bitmap?, isCoverSize: Boolean = true, onFinish: (Bitmap?) -> (Unit)) {
        setImageCoverAsync(context, manga, isCoverSize) {
            val image = it ?: notLocate
            for (imageView in imagesView)
                imageView.setImageBitmap(image)

            onFinish(image)
        }
    }

    fun setImageCoverAsync(
        context: Context,
        manga: Manga,
        imageView: ImageView,
        notLocate: Bitmap?,
        isCoverSize: Boolean = true,
        onFinish: (Bitmap?) -> (Unit)
    ) {
        setImageCoverAsync(context, manga, isCoverSize) {
            val image = it ?: notLocate
            imageView.setImageBitmap(image)
            onFinish(image)
        }
    }

}