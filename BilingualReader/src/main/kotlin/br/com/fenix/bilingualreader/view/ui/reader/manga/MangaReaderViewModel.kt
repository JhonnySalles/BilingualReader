package br.com.fenix.bilingualreader.view.ui.reader.manga

import android.app.Application
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.model.entity.Chapters
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.service.parses.manga.RarParse
import br.com.fenix.bilingualreader.service.repository.MangaAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.SharedData
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.ImageUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import com.squareup.picasso.Transformation
import jp.wasabeef.picasso.transformations.ColorFilterTransformation
import jp.wasabeef.picasso.transformations.GrayscaleTransformation
import jp.wasabeef.picasso.transformations.gpu.InvertFilterTransformation
import jp.wasabeef.picasso.transformations.gpu.SepiaFilterTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File

class MangaReaderViewModel(var app: Application) : AndroidViewModel(app) {

    private var mPreferences: SharedPreferences = GeneralConsts.getSharedPreferences(app.applicationContext)
    private val mLOGGER = LoggerFactory.getLogger(MangaReaderViewModel::class.java)

    private val mAnnotationRepository = MangaAnnotationRepository(app.applicationContext)

    // --------------------------------------------------------- Colors / Layout ---------------------------------------------------------
    private var mFilters: MutableLiveData<MutableList<Transformation>> = MutableLiveData(arrayListOf())
    val filters: LiveData<MutableList<Transformation>> = mFilters

    private var mCustomFilter: MutableLiveData<Boolean> = MutableLiveData(false)
    val customFilter: LiveData<Boolean> = mCustomFilter

    private var mColorRed: MutableLiveData<Int> = MutableLiveData(0)
    val colorRed: LiveData<Int> = mColorRed
    private var mColorGreen: MutableLiveData<Int> = MutableLiveData(0)
    val colorGreen: LiveData<Int> = mColorGreen
    private var mColorBlue: MutableLiveData<Int> = MutableLiveData(0)
    val colorBlue: LiveData<Int> = mColorBlue
    private var mColorAlpha: MutableLiveData<Int> = MutableLiveData(0)
    val colorAlpha: LiveData<Int> = mColorAlpha

    private var mGrayScale: MutableLiveData<Boolean> = MutableLiveData(false)
    val grayScale: LiveData<Boolean> = mGrayScale
    private var mInvertColor: MutableLiveData<Boolean> = MutableLiveData(false)
    val invertColor: LiveData<Boolean> = mInvertColor
    private var mBlueLight: MutableLiveData<Boolean> = MutableLiveData(false)
    val blueLight: LiveData<Boolean> = mBlueLight
    private var mBlueLightAlpha: MutableLiveData<Int> = MutableLiveData(100)
    val blueLightAlpha: LiveData<Int> = mBlueLightAlpha
    private var mSepia: MutableLiveData<Boolean> = MutableLiveData(false)
    val sepia: LiveData<Boolean> = mSepia

    private var mOcrItem: MutableLiveData<ArrayList<String>> = MutableLiveData(arrayListOf())
    var ocrItem: LiveData<ArrayList<String>> = mOcrItem
    private var mAnnotation: MutableLiveData<MutableList<MangaAnnotation>> = MutableLiveData(mutableListOf<MangaAnnotation>())
    val annotation: LiveData<MutableList<MangaAnnotation>> = mAnnotation

    var history: History? = null

    var mLanguageOcr: Languages? = null
    var mIsAlertSubtitle = false

    private var mBlueLightColor = Color.argb(mBlueLightAlpha.value!!, 255, 50, 0)

    init {
        loadPreferences()
    }

    fun stopExecutions() {
        stopLoadChapters = true
        stopLoadAnnotation = true
    }

    fun clear() {
        mLanguageOcr = null
        mAnnotation.value = mutableListOf<MangaAnnotation>()
    }

    fun changeCustomFilter(value: Boolean) {
        mCustomFilter.value = value
        generateFilters()
    }

    fun changeGrayScale(value: Boolean) {
        mGrayScale.value = value
        generateFilters()
    }

    fun changeInvertColor(value: Boolean) {
        mInvertColor.value = value
        generateFilters()
    }

    fun changeBlueLight(value: Boolean) {
        mBlueLight.value = value
        generateFilters()
    }

    fun changeSepia(value: Boolean) {
        mSepia.value = value
        generateFilters()
    }

    fun changeColorsFilter(red: Int, green: Int, blue: Int, alpha: Int) {
        mColorRed.value = red
        mColorGreen.value = green
        mColorBlue.value = blue
        mColorAlpha.value = alpha
        generateFilters()
    }

    fun changeBlueLightAlpha(value: Int) {
        mBlueLightAlpha.value = value
        mBlueLightColor = Color.argb(mBlueLightAlpha.value!!, 255, 50, 0)
        generateFilters()
    }

    fun clearOcrItem() {
        mOcrItem.value = arrayListOf()
    }

    fun addOcrItem(text: ArrayList<String>) {
        mOcrItem.value?.addAll(text)
        mOcrItem.value = mOcrItem.value // Force live data in add item
    }

    fun addOcrItem(text: String?) {
        if (text == null || mOcrItem.value == null) return

        if (!mOcrItem.value!!.contains(text)) {
            mOcrItem.value!!.add(text)
            mOcrItem.value = mOcrItem.value // Force live data in add item
        }
    }

    private fun loadPreferences() {
        mCustomFilter.value = mPreferences.getBoolean(
            GeneralConsts.KEYS.COLOR_FILTER.CUSTOM_FILTER,
            false
        )

        mColorRed.value = mPreferences.getInt(
            GeneralConsts.KEYS.COLOR_FILTER.COLOR_RED,
            0
        )

        mColorGreen.value = mPreferences.getInt(
            GeneralConsts.KEYS.COLOR_FILTER.COLOR_BLUE,
            0
        )

        mColorBlue.value = mPreferences.getInt(
            GeneralConsts.KEYS.COLOR_FILTER.COLOR_GREEN,
            0
        )

        mColorAlpha.value = mPreferences.getInt(
            GeneralConsts.KEYS.COLOR_FILTER.COLOR_ALPHA,
            0
        )

        mGrayScale.value = mPreferences.getBoolean(
            GeneralConsts.KEYS.COLOR_FILTER.GRAY_SCALE,
            false
        )

        mInvertColor.value = mPreferences.getBoolean(
            GeneralConsts.KEYS.COLOR_FILTER.INVERT_COLOR,
            false
        )

        mSepia.value = mPreferences.getBoolean(
            GeneralConsts.KEYS.COLOR_FILTER.SEPIA,
            false
        )

        mBlueLight.value = mPreferences.getBoolean(
            GeneralConsts.KEYS.COLOR_FILTER.BLUE_LIGHT,
            false
        )

        mBlueLightAlpha.value = mPreferences.getInt(
            GeneralConsts.KEYS.COLOR_FILTER.BLUE_LIGHT_ALPHA,
            100
        )

        generateFilters()
    }

    private fun savePreferences() {
        with(mPreferences.edit()) {
            this.putBoolean(
                GeneralConsts.KEYS.COLOR_FILTER.CUSTOM_FILTER,
                mCustomFilter.value!!
            )
            this.putInt(
                GeneralConsts.KEYS.COLOR_FILTER.COLOR_RED,
                mColorRed.value!!
            )
            this.putInt(
                GeneralConsts.KEYS.COLOR_FILTER.COLOR_BLUE,
                mColorGreen.value!!
            )
            this.putInt(
                GeneralConsts.KEYS.COLOR_FILTER.COLOR_GREEN,
                mColorBlue.value!!
            )
            this.putInt(
                GeneralConsts.KEYS.COLOR_FILTER.COLOR_ALPHA,
                mColorAlpha.value!!
            )
            this.putBoolean(
                GeneralConsts.KEYS.COLOR_FILTER.GRAY_SCALE,
                mGrayScale.value!!
            )
            this.putBoolean(
                GeneralConsts.KEYS.COLOR_FILTER.INVERT_COLOR,
                mInvertColor.value!!
            )
            this.putBoolean(
                GeneralConsts.KEYS.COLOR_FILTER.SEPIA,
                mSepia.value!!
            )
            this.putBoolean(
                GeneralConsts.KEYS.COLOR_FILTER.BLUE_LIGHT,
                mBlueLight.value!!
            )
            this.putInt(
                GeneralConsts.KEYS.COLOR_FILTER.BLUE_LIGHT_ALPHA,
                mBlueLightAlpha.value!!
            )
            this.commit()
        }
    }

    private fun generateFilters() {
        savePreferences()
        val filters: MutableList<Transformation> = arrayListOf()

        if (mCustomFilter.value!!) {
            val color = Color.argb(mColorAlpha.value!!, mColorRed.value!!, mColorGreen.value!!, mColorBlue.value!!)
            filters.add(ColorFilterTransformation(color))
        }

        if (mBlueLight.value!!)
            filters.add(ColorFilterTransformation(mBlueLightColor))

        if (mGrayScale.value!!)
            filters.add(GrayscaleTransformation())

        if (mInvertColor.value!!)
            filters.add(InvertFilterTransformation(app.applicationContext))

        if (mSepia.value!!)
            filters.add(SepiaFilterTransformation(app.applicationContext))

        mFilters.value = filters
    }

    // --------------------------------------------------------- Chapters ---------------------------------------------------------
    var isLoadChapters = false
    private var stopLoadChapters = false
    private fun loadImage(parse: Parse, page: Int, isSmallSize: Boolean = true) : Bitmap? {
        try {
            var stream = parse.getPage(page)
            val image = if (isSmallSize) {
                val option = BitmapFactory.Options()
                option.inJustDecodeBounds = true
                BitmapFactory.decodeStream(stream, null, option)
                option.inSampleSize = ImageUtil.calculateInSampleSize(
                    option,
                    ReaderConsts.PAGE.PAGE_CHAPTER_LIST_WIDTH,
                    ReaderConsts.PAGE.PAGE_CHAPTER_LIST_HEIGHT
                )
                option.inJustDecodeBounds = false
                Util.closeInputStream(stream)

                stream = parse.getPage(page)
                BitmapFactory.decodeStream(stream, null, option)
            } else
                BitmapFactory.decodeStream(stream)

            Util.closeInputStream(stream)
            return image
        } catch (m: OutOfMemoryError) {
            System.gc()
            mLOGGER.error("Memory full, cleaning", m)
        } catch (e: Exception) {
            mLOGGER.error("Error to load image page", e)
        }
        return null
    }

    fun loadChapter(manga: Manga?, number: Int): Boolean {
        if (manga == null) {
            SharedData.clearChapters()
            return false
        }

        if (SharedData.isProcessed(manga)) {
            SharedData.clearChapters()
            val parse = ParseFactory.create(manga.file) ?: return false
            isLoadChapters = true
            stopLoadChapters = false

            if (parse is RarParse) {
                val cacheDir = File(GeneralConsts.getCacheDir(app.applicationContext), GeneralConsts.CACHE_FOLDER.IMAGE)
                (parse as RarParse?)!!.setCacheDirectory(cacheDir)
            }

            val list = arrayListOf<Chapters>()
            val chapters = parse.getPagePaths()
            var title = Chapters("", 0, 0, 0f, true)

            for (i in 0 until parse.numPages()) {
                val chapter = chapters.entries.filter { it.value <= i }.sortedByDescending{ it.value }.first()
                if (title.chapter != chapter.value.toFloat()) {
                    title = Chapters(chapter.key, i, i + 1, chapter.value.toFloat(), true)
                    list.add(title)
                }

                list.add(Chapters(Util.getNameFromPath(parse.getPagePath(i) ?: ""), i, i+1, 0f, false, isSelected = number == i+1))
            }

            SharedData.setChapters(manga, list)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val deferred = async {
                        if (number > 5) {
                            for (i in (number - 3) until list.size) {
                                if (stopLoadChapters)
                                    break

                                val page = list[i]
                                page.image = loadImage(parse, page.number, false)
                                withContext(Dispatchers.Main) { SharedData.callListeners(page.number) }
                            }

                            for (i in (number - 4) downTo 0) {
                                if (stopLoadChapters)
                                    break

                                val page = list[i]
                                page.image = loadImage(parse, page.number, false)
                                withContext(Dispatchers.Main) { SharedData.callListeners(page.number) }
                            }
                        } else
                            for (page in list) {
                                if (stopLoadChapters)
                                    break

                                page.image = loadImage(parse, page.number, false)
                                withContext(Dispatchers.Main) { SharedData.callListeners(page.number) }
                            }
                    }

                    deferred.await()
                    withContext(Dispatchers.Main) {
                        if (!stopLoadChapters)
                            SharedData.setChapters(manga, list.toList())
                    }
                    isLoadChapters = false
                } finally {
                    Util.destroyParse(parse)
                }
            }
        }

        return true
    }

    // --------------------------------------------------------- Manga - Annottation ---------------------------------------------------------
    fun save(annotation: MangaAnnotation) {
        if (annotation.id == null)
            annotation.id = mAnnotationRepository.save(annotation)
        else
            mAnnotationRepository.update(annotation)

        val list = mAnnotation.value ?: mutableListOf()
        if (list.none { it.page == annotation.page })
            list.add(annotation)

        mAnnotation.value = generateTitle(list.filter { !it.isTitle }.sortedBy { it.page })
    }

    fun delete(annotation: MangaAnnotation) {
        if (annotation.id != null)
            mAnnotationRepository.delete(annotation)

        val list = mAnnotation.value ?: mutableListOf()
        if (!list.any { it.page == annotation.page })
            list.removeIf { it.page == annotation.page }

        mAnnotation.value = generateTitle(list.filter { !it.isTitle }.sortedBy { it.page })
    }

    fun deleteAnnotation(position : Int) {
        val annotation = mAnnotation.value?.removeAt(position) ?: return

        if (annotation.id != null)
            mAnnotationRepository.delete(annotation)

        val list = mAnnotation.value ?: mutableListOf()
        mAnnotation.value = generateTitle(list.filter { !it.isTitle }.sortedBy { it.page })
    }

    private fun generateTitle(annotations: List<MangaAnnotation>) : MutableList<MangaAnnotation>{
        val list = mutableListOf<MangaAnnotation>()
        if (annotations.isNotEmpty()) {
            val annotation = annotations[0]
            val idMaga = annotation.id_parent
            var title = MangaAnnotation(idMaga, annotation.chapter, annotation.folder, "", isRoot = true, isTitle = true)
            list.add(title)
            for (annotation in annotations) {
                if (list.none { it.chapter.equals(annotation.chapter, ignoreCase = true) }) {
                    title = MangaAnnotation(idMaga, annotation.chapter, annotation.folder, "", isRoot = true, isTitle = true)
                    list.add(title)
                }
                title.count++
                list.add(annotation)
            }
        }
        return list
    }

    fun refreshAnnotations(manga: Manga?) {
        var annotations = mutableListOf<MangaAnnotation>()
        try {
            if (manga == null || manga.id == null)
                return

            annotations = generateTitle(findAnnotationByMaga(manga).sortedBy { it.page })
        } finally {
            mAnnotation.value = annotations
        }
    }

    private var stopLoadAnnotation = false
    fun refreshCover(manga: Manga?, refresh: (Int) -> (Unit)) {
        val list = mAnnotation.value ?: mutableListOf()
        if (manga == null || list.none { it.image == null })
            return

        val parse = ParseFactory.create(manga.file) ?: return
        stopLoadAnnotation = false

        if (parse is RarParse) {
            val cacheDir = File(GeneralConsts.getCacheDir(app.applicationContext), GeneralConsts.CACHE_FOLDER.IMAGE)
            (parse as RarParse?)!!.setCacheDirectory(cacheDir)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deferred = async {
                    for (annotation in list.filter { it.image == null && !it.isTitle }) {
                        annotation.image = loadImage(parse, annotation.page -1, false)
                        withContext(Dispatchers.Main) { refresh(annotation.page -1) }
                        if (stopLoadAnnotation)
                            break
                    }
                }
                deferred.await()
                withContext(Dispatchers.Main) {
                    if (!stopLoadAnnotation)
                        mAnnotation.value = list
                }
            } finally {
                Util.destroyParse(parse)
            }
        }
    }

    fun findAnnotationByMaga(manga: Manga) = mAnnotationRepository.findByManga(manga.id!!)

    fun findAnnotationByPage(manga: Manga, page: Int) = mAnnotationRepository.findByPage(manga.id!!, page)

}