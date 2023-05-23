package br.com.fenix.bilingualreader.view.ui.reader.book

import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import android.graphics.Color
import android.view.View
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.*
import br.com.fenix.bilingualreader.model.enums.*
import br.com.fenix.bilingualreader.service.controller.WebInterface
import br.com.fenix.bilingualreader.service.japanese.Formatter
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.VocabularyRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.FontUtil
import br.com.fenix.bilingualreader.util.helpers.TextUtil
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

class BookReaderViewModel(var app: Application) : AndroidViewModel(app) {

    private var mPreferences: SharedPreferences = GeneralConsts.getSharedPreferences(app.applicationContext)
    private val mRepository: BookRepository = BookRepository(app.applicationContext)
    private val mVocabularyRepository = VocabularyRepository(app.applicationContext)

    private val mLOGGER = LoggerFactory.getLogger(BookReaderViewModel::class.java)

    // --------------------------------------------------------- Fonts / Layout ---------------------------------------------------------
    private var mListFonts: MutableLiveData<MutableList<Pair<FontType, Boolean>>> = MutableLiveData(arrayListOf())
    val fonts: LiveData<MutableList<Pair<FontType, Boolean>>> = mListFonts

    private var mAlignmentType: MutableLiveData<AlignmentLayoutType> = MutableLiveData(AlignmentLayoutType.Justify)
    val alignmentType: LiveData<AlignmentLayoutType> = mAlignmentType

    private var mMarginType: MutableLiveData<MarginLayoutType> = MutableLiveData(MarginLayoutType.Small)
    val marginType: LiveData<MarginLayoutType> = mMarginType

    private var mSpacingType: MutableLiveData<SpacingLayoutType> = MutableLiveData(SpacingLayoutType.Small)
    val spacingType: LiveData<SpacingLayoutType> = mSpacingType

    private var mScrollingType: MutableLiveData<ScrollingType> = MutableLiveData(ScrollingType.Pagination)
    val scrollingType: LiveData<ScrollingType> = mScrollingType

    private var mFontType: MutableLiveData<FontType> = MutableLiveData(FontType.TimesNewRoman)
    val fontType: LiveData<FontType> = mFontType

    private var mFontSize: MutableLiveData<Float> = MutableLiveData(GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT)
    val fontSize: LiveData<Float> = mFontSize

    private var mFontCss: MutableLiveData<String> = MutableLiveData("")
    val fontCss: LiveData<String> = mFontCss

    private var mFontsLocation: String = FontType.getCssFont()
    private var mDefaultCss : String = ""
    var mWebFontSize = FontUtil.pixelToDips(app.applicationContext, fontSize.value!!)
    var isJapanese = true
    private var isProcessJapaneseText = mPreferences.getBoolean(GeneralConsts.KEYS.READER.BOOK_PROCESS_JAPANESE_TEXT, true)
    private var isFurigana = mPreferences.getBoolean(GeneralConsts.KEYS.READER.BOOK_GENERATE_FURIGANA_ON_TEXT, true)

    init {
        loadPreferences(false)
    }

    fun getDefaultCSS() : String {
        if (mDefaultCss.isEmpty())
            mDefaultCss = generateCSS()
        return mDefaultCss
    }

    private fun generateCSS(): String {
        val fontColor = if(app.resources.getBoolean(R.bool.isNight)) "#ffffff" else "#000000"

        val fontType = fontType.value?.name ?: FontType.TimesNewRoman.name
        val fontSize = FontUtil.pixelToDips(app.applicationContext, fontSize.value!!).toString() + "px"

        val margin = when (marginType.value) {
            MarginLayoutType.Small -> "10px"
            MarginLayoutType.Medium -> "25px"
            MarginLayoutType.Big -> "40px"
            else -> "10px"
        }

        val spacing = when (spacingType.value) {
            SpacingLayoutType.Small -> "140%"
            SpacingLayoutType.Medium -> "160%"
            SpacingLayoutType.Big -> "180%"
            else -> "140%"
        }

        val alignment = when (alignmentType.value) {
            AlignmentLayoutType.Justify -> "justify"
            AlignmentLayoutType.Right -> "right"
            AlignmentLayoutType.Left -> "left"
            AlignmentLayoutType.Center -> "center"
            else -> "justify"
        }

        val img = "img {" +
                "  max-width: 100%;" +
                "  max-height: 100%;" +
                "}"

        val meta = "<meta name=\"viewport\" content=\"height=device-height, width=device-width, initial-scale=1, maximum-scale=2, user-scalable=yes\" >"
        val style = "<style type=\"text/css\"> " +
                mFontsLocation +
                "body { " +
                "  font-family: $fontType, times; " +
                "  font-size: $fontSize; " +
                "  color: $fontColor; " +
                "  line-height: $spacing; " +
                "  text-align: $alignment; " +
                "  margin: $margin $margin $margin $margin; " +
               // (if (isJapanese) " -webkit-text-orientation: upright; -webkit-writing-mode: vertical-rl;" else "") + // Not working
                "} " +
                img +
                Formatter.getCss() +
                "</style>"
        mFontCss.value = style
        return "<head>$meta$style</head>"
    }

    fun loadConfiguration(book: Book?) {
        isJapanese = book?.language == Languages.JAPANESE
        val config : BookConfiguration? = if (book?.id != null) mRepository.findConfiguration(book.id!!) else null
        loadConfiguration(config)

        if (book?.id != null) {
            if (config == null)
                saveBookConfiguration(book.id!!)
            mVocabularyRepository.processVocabulary(app.applicationContext, book.id!!)
        }
    }

    private fun loadPreferences(isJapanese: Boolean) {
        mAlignmentType.value = AlignmentLayoutType.valueOf(
            mPreferences.getString(
                GeneralConsts.KEYS.READER.BOOK_PAGE_ALIGNMENT,
                AlignmentLayoutType.Justify.toString()
            )!!
        )

        mMarginType.value = MarginLayoutType.valueOf(
            mPreferences.getString(
                GeneralConsts.KEYS.READER.BOOK_PAGE_MARGIN,
                MarginLayoutType.Small.toString()
            )!!
        )

        mSpacingType.value = SpacingLayoutType.valueOf(
            mPreferences.getString(
                GeneralConsts.KEYS.READER.BOOK_PAGE_SPACING,
                SpacingLayoutType.Small.toString()
            )!!
        )

        val typeKey = if (isJapanese) GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_TYPE_JAPANESE else GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_TYPE_NORMAL
        val typeDefault = if (isJapanese) FontType.BabelStoneErjian1.toString() else FontType.TimesNewRoman.toString()
        mFontType.value = FontType.valueOf(
            mPreferences.getString(
                typeKey,
                typeDefault
            )!!
        )

        isFurigana = mPreferences.getBoolean(
            GeneralConsts.KEYS.READER.BOOK_GENERATE_FURIGANA_ON_TEXT,
            true
        )

        isProcessJapaneseText = mPreferences.getBoolean(
            GeneralConsts.KEYS.READER.BOOK_PROCESS_JAPANESE_TEXT,
            true
        )

        mFontSize.value = mPreferences.getFloat(
            GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE,
            GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT
        )

        mDefaultCss = generateCSS()
    }

    fun loadConfiguration(configuration: BookConfiguration?) {
        if (configuration == null)
            loadPreferences(isJapanese)
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
            AlignmentLayoutType.Justify,
            MarginLayoutType.Small,
            SpacingLayoutType.Small,
            FontType.TimesNewRoman,
            GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT,
            ScrollingType.Pagination
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

    fun setSelectAlignment(alignment: AlignmentLayoutType) {
        mAlignmentType.value = alignment
        mDefaultCss = generateCSS()
    }

    fun setSelectSpacing(spacing: SpacingLayoutType) {
        mSpacingType.value = spacing
        mDefaultCss = generateCSS()
    }

    fun setSelectMargin(margin: MarginLayoutType) {
        mMarginType.value = margin
        mDefaultCss = generateCSS()
    }

    @SuppressLint("JavascriptInterface")
    fun prepareHtml(parse: DocumentParse?, page: Int, web: WebView, listener : View.OnTouchListener? = null, javascript: WebInterface? = null) {
        var text = parse?.getPage(page)?.pageHTMLWithImages.orEmpty()

        if (text.contains("<image-begin>image"))
            text = text.replace("<image-begin>", "<img src=\"data:")
                .replace("<image-end>", "\" />")

        text = TextUtil.formatHtml(text)

        if (isProcessJapaneseText && isJapanese)
            text = Formatter.generateHtmlText(text, isFurigana)

        val html =
            "<!DOCTYPE html><html>" + getDefaultCSS() + "<body><div id=\"text\">" +
                    text +
                    "</div>" +
                    "</body></html>"

        parse?.getPage(page)?.recycle()

        web.loadDataWithBaseURL("file:///android_res/", html, "text/html", "UTF-8", null)

        if (listener != null)
            web.setOnTouchListener(listener)

        web.settings.javaScriptEnabled = true
        web.settings.defaultFontSize = mWebFontSize
        web.setLayerType(WebView.LAYER_TYPE_NONE, null)

        web.settings.setSupportZoom(true)
        web.settings.builtInZoomControls = true
        web.settings.displayZoomControls = false
        web.settings.loadWithOverviewMode = true
        web.settings.useWideViewPort = true

        web.setBackgroundColor(Color.TRANSPARENT)
        // Use variable android in javascript to call functions java
        if (javascript != null)
            web.addJavascriptInterface(javascript, "bilingualapp")
    }

}