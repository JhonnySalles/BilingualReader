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
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.service.parses.manga.Parse
import br.com.fenix.bilingualreader.service.parses.manga.ParseFactory
import br.com.fenix.bilingualreader.service.parses.manga.RarParse
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

    // --------------------------------------------------------- Fonts / Layout ---------------------------------------------------------
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

    var history: History? = null

    var mLanguageOcr: Languages? = null
    var mIsAlertSubtitle = false

    private var mBlueLightColor = Color.argb(mBlueLightAlpha.value!!, 255, 50, 0)

    init {
        loadPreferences()
    }

    fun clear() {
        mLanguageOcr = null
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
    private fun loadImage(parse: Parse, page: Int) : Bitmap? {
        try {
            var stream = parse.getPage(page)
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
            val image = BitmapFactory.decodeStream(stream, null, option)
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
            val parse = ParseFactory.create(manga.file) ?: return false

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
                val deferred = async {
                    if (number > 5) {
                        for (i in (number - 3) until list.size) {
                            val page = list[i]
                            page.image = loadImage(parse, page.number)
                            withContext(Dispatchers.Main) { SharedData.callListeners(page.number) }
                        }

                        for (i in (number - 4) downTo 0) {
                            val page = list[i]
                            page.image = loadImage(parse, page.number)
                            withContext(Dispatchers.Main) { SharedData.callListeners(page.number) }
                        }
                    } else
                        for (page in list) {
                            page.image = loadImage(parse, page.number)
                            withContext(Dispatchers.Main) { SharedData.callListeners(page.number) }
                        }

                    Util.destroyParse(parse)
                }

                deferred.await()
                withContext(Dispatchers.Main) {
                    SharedData.setChapters(manga, list.toList())
                }
            }
        }

        return true
    }

}