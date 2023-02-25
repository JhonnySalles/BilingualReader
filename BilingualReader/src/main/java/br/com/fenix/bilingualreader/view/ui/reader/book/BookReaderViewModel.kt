package br.com.fenix.bilingualreader.view.ui.reader.book

import android.app.Application
import android.content.SharedPreferences
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.widget.TextView
import androidx.appcompat.widget.TintTypedArray
import androidx.appcompat.widget.TintTypedArray.obtainStyledAttributes
import androidx.core.content.withStyledAttributes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookConfiguration
import br.com.fenix.bilingualreader.model.enums.*
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.FontUtil
import org.slf4j.LoggerFactory

class BookReaderViewModel(var app: Application) : AndroidViewModel(app) {

    private var mPreferences: SharedPreferences =
        GeneralConsts.getSharedPreferences(app.applicationContext)
    private val mRepository: BookRepository = BookRepository(app.applicationContext)

    private val mLOGGER = LoggerFactory.getLogger(BookReaderViewModel::class.java)

    private var mListFonts: MutableLiveData<MutableList<Pair<FontType, Boolean>>> = MutableLiveData(arrayListOf())
    val fonts: LiveData<MutableList<Pair<FontType, Boolean>>> = mListFonts

    private var mAlignmentType: MutableLiveData<AlignmentType> =
        MutableLiveData(AlignmentType.Center)
    val alignmentType: LiveData<AlignmentType> = mAlignmentType

    private var mMarginType: MutableLiveData<MarginType> = MutableLiveData(MarginType.Small)
    val marginType: LiveData<MarginType> = mMarginType

    private var mSpacingType: MutableLiveData<SpacingType> = MutableLiveData(SpacingType.Small)
    val spacingType: LiveData<SpacingType> = mSpacingType

    private var mScrollingType: MutableLiveData<ScrollingType> =
        MutableLiveData(ScrollingType.Pagination)
    val scrollingType: LiveData<ScrollingType> = mScrollingType

    private var mFontType: MutableLiveData<FontType> = MutableLiveData(FontType.TimesNewRoman)
    val fontType: LiveData<FontType> = mFontType

    private var mFontSize: MutableLiveData<Float> = MutableLiveData(
        GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT
    )
    val fontSize: LiveData<Float> = mFontSize

    private var mFontCss: MutableLiveData<String> = MutableLiveData("")
    val fontCss: LiveData<String> = mFontCss

    private var mFontsLocation: String = FontType.getCssFont()
    private var mDefaultCss : String = ""
    var mWebFontSize = FontUtil.pixelToDips(app.applicationContext, fontSize.value!!)

    init {
        loadPreferences()
    }

    fun getDefaultCSS() : String {
        if (mDefaultCss.isEmpty())
            mDefaultCss = generateCSS()
        return mDefaultCss
    }

    private fun generateCSS(): String {
        //Not use #, because webview not showing, use %23 for #
        val fontColor = if(app.resources.getBoolean(R.bool.isNight)) "#ffffff" else "#000000"

        val fontType = fontType.value?.name ?: FontType.TimesNewRoman.name
        val fontSize =  FontUtil.pixelToDips(app.applicationContext, fontSize.value!!).toString() + "px"

        val margin = when (marginType.value) {
            MarginType.Small -> "1px"
            MarginType.Medium -> "3px"
            MarginType.Big -> "6px"
            else -> "1px"
        }

        val spacing = when (spacingType.value) {
            SpacingType.Small -> "140%"
            SpacingType.Medium -> "160%"
            SpacingType.Big -> "180%"
            else -> "140%"
        }

        val alignment = when (alignmentType.value) {
            AlignmentType.Center -> "justify"
            AlignmentType.Right -> "right"
            AlignmentType.Left -> "left"
            else -> "justify"
        }

        val style = "<style type=\"text/css\"> " +
                mFontsLocation +
                "body { " +
                "  font-family: $fontType, times; " +
                "  font-size: $fontSize; " +
                "  color: $fontColor; " +
                "  line-height: $spacing; " +
                "  text-align: $alignment; " +
                "  margin: $margin $margin 50px $margin; " +
                "  margin-bottom: 50px; " +
                "} " +
                "</style>"
        mFontCss.value = style
        return "<head>$style</head>"
    }

    private fun loadPreferences() {
        mAlignmentType.value = AlignmentType.valueOf(
            mPreferences.getString(
                GeneralConsts.KEYS.READER.BOOK_PAGE_ALIGNMENT,
                AlignmentType.Center.toString()
            )!!
        )

        mMarginType.value = MarginType.valueOf(
            mPreferences.getString(
                GeneralConsts.KEYS.READER.BOOK_PAGE_MARGIN,
                MarginType.Small.toString()
            )!!
        )

        mSpacingType.value = SpacingType.valueOf(
            mPreferences.getString(
                GeneralConsts.KEYS.READER.BOOK_PAGE_SPACING,
                SpacingType.Small.toString()
            )!!
        )

        mScrollingType.value = ScrollingType.valueOf(
            mPreferences.getString(
                GeneralConsts.KEYS.READER.BOOK_PAGE_SCROLLING_MODE,
                ScrollingType.Pagination.toString()
            )!!
        )

        mFontType.value = FontType.valueOf(
            mPreferences.getString(
                GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_TYPE,
                FontType.TimesNewRoman.toString()
            )!!
        )

        mFontSize.value = mPreferences.getFloat(
            GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE,
            GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT
        )

        mDefaultCss = generateCSS()
    }

    fun loadDefaultConfiguration() {
        loadPreferences()
    }

    fun loadBookConfiguration(configuration: BookConfiguration?) {
        if (configuration == null)
            loadPreferences()
        else {
            mAlignmentType.value = configuration.alignment
            mMarginType.value = configuration.margin
            mScrollingType.value = configuration.scrolling
            mSpacingType.value = configuration.spacing
            mFontType.value = configuration.fontType
            mFontSize.value = configuration.fontSize
            mDefaultCss = generateCSS()
        }
    }


    fun saveBookConfiguration(idBook: Long) {
        val config = mRepository.findConfiguration(idBook) ?: BookConfiguration(
            null,
            idBook,
            AlignmentType.Center,
            MarginType.Small,
            SpacingType.Small,
            ScrollingType.Pagination,
            FontType.TimesNewRoman,
            GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT
        )
        saveBookConfiguration(config)
    }

    fun saveBookConfiguration(configuration: BookConfiguration) {
        configuration.alignment = alignmentType.value!!
        configuration.margin = marginType.value!!
        configuration.scrolling = scrollingType.value!!
        configuration.spacing = spacingType.value!!
        configuration.fontType = fontType.value!!
        configuration.fontSize = fontSize.value!!

        if (configuration.id == null)
            mRepository.saveConfiguration(configuration)
        else
            mRepository.updateConfiguration(configuration)
    }

    fun loadFonts() {
        mListFonts.value = FontType.values().map { Pair(it, it == mFontType.value) }.toMutableList()
    }

    fun getSelectedFontTypeIndex(): Int  {
        val index = mListFonts.value?.indexOfFirst { it.second } ?: 0
        return if (index == -1) 0 else index
    }

    fun setSelectFont(font: FontType) {
        mFontType.value = font
        if (mListFonts.value != null)
            mListFonts.value = mListFonts.value!!.map { Pair(it.first, it.first == font) }.toMutableList()
        mDefaultCss = generateCSS()
    }

    fun changeFontSize(value : Float) {
        mFontSize.value = value
        mDefaultCss = generateCSS()
    }

}