package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.service.parses.manga.RarParse
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
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
import java.io.InputStream


class MangaImageCoverController private constructor() {

    companion object {
        val instance: MangaImageCoverController by lazy { HOLDER.INSTANCE }
        val thread: CoroutineDispatcher = java.util.concurrent.Executors.newFixedThreadPool(3).asCoroutineDispatcher()
    }

    private val mLOGGER = LoggerFactory.getLogger(MangaImageCoverController::class.java)

    private object HOLDER {
        val INSTANCE = MangaImageCoverController()
    }

    private fun saveBitmapToCache(context: Context, key: String, bitmap: Bitmap) {
        try {
            val cacheDir = File(GeneralConsts.getCoverDir(context), GeneralConsts.CACHE_FOLDER.MANGA_COVERS)
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
            val file = File(GeneralConsts.getCoverDir(context), GeneralConsts.CACHE_FOLDER.MANGA_COVERS + '/' + key)

            if (file.exists()) {
                return ImageUtil.decodeFile(file) ?: return null
            }
        } catch (e: Exception) {
            mLOGGER.error("Error retrieve bitmap from cache: " + e.message, e)
            Telemetry.recordException(e, "Error retrieve bitmap from cache: " + e.message)
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
        val stream: InputStream? = parse.getCover().first

        val cover: Bitmap?

        if (isCoverSize) {
            cover = stream?.use {
                ImageUtil.decodeInputStream(
                    it,
                    ReaderConsts.COVER.MANGA_COVER_THUMBNAIL_WIDTH,
                    ReaderConsts.COVER.MANGA_COVER_THUMBNAIL_HEIGHT
                )
            }
            if (cover != null)
                saveBitmapToCache(context, hash, cover)
        } else {
            cover = stream?.use {
                ImageUtil.decodeInputStream(it)
            }
        }

        return cover
    }

    fun getMangaCoverFile(context: Context, manga: Manga, isCoverSize: Boolean): File? {
        val hash = generateHash(manga.file)
        val cacheDir = File(GeneralConsts.getCoverDir(context), GeneralConsts.CACHE_FOLDER.MANGA_COVERS)
        val cacheFile = File(cacheDir, hash)

        if (isCoverSize && cacheFile.exists()) {
            return cacheFile
        }

        if (!manga.file.exists()) {
            return null
        }

        val parse = ParseFactory.create(manga.file) ?: return null
        try {
            if (parse is RarParse) {
                val folder = GeneralConsts.CACHE_FOLDER.RAR + '/' + Util.normalizeNameCache(manga.file.nameWithoutExtension)
                val cacheDirRar = File(GeneralConsts.getCacheDir(context), folder)
                (parse as RarParse?)!!.setCacheDirectory(cacheDirRar)
            }

            val cover = getCoverFromFile(context, hash, parse, isCoverSize)
            if (cover != null) {
                return cacheFile
            }
        } catch (e: Exception) {
            mLOGGER.error("Error getMangaCoverFile: " + e.message, e)
        } finally {
            Util.destroyParse(parse)
        }

        return null
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
                val image: Bitmap? = getMangaCover(context, manga, isCoverSize)
                withContext(Dispatchers.Main) {
                    function(image)
                }
            } catch (m: OutOfMemoryError) {
                System.gc()
                mLOGGER.error("Memory full, cleaning", m)
            } catch (m: IOException) {
                mLOGGER.error("Error to load image async: " + manga.name, m)
                Telemetry.recordException(m, "Error to load image async: " + m.message)
            } catch (e: FileNotFoundException) {
                mLOGGER.error("File not found. Error to load image async: " + manga.name, e)
            } catch (e: Exception) {
                mLOGGER.error("Error to load image async: " + manga.name, e)
                Telemetry.recordException(e, "Error to load image async: " + e.message)
            }
        }
    }

    fun setImageCoverAsync(context: Context, manga: Manga, imageView: ImageView, notLocate: Bitmap?, @Suppress("UNUSED_PARAMETER") isCoverSize: Boolean = true) {
        imageView.load(manga) {
            allowHardware(false)
            crossfade(true)
            if (notLocate != null) {
                placeholder(BitmapDrawable(context.resources, notLocate))
                error(BitmapDrawable(context.resources, notLocate))
            }
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
        @Suppress("UNUSED_PARAMETER") isCoverSize: Boolean = true,
        onFinish: (Bitmap?) -> (Unit)
    ) {
        imageView.load(manga) {
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