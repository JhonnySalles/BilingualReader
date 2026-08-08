package br.com.fenix.bilingualreader.view.ui.reader.book

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.text.LineBreaker.JUSTIFICATION_MODE_INTER_WORD
import android.os.Build
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.BookConfiguration
import br.com.fenix.bilingualreader.model.entity.Chapters
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.enums.AlignmentLayoutType
import br.com.fenix.bilingualreader.model.enums.FontType
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.MarginLayoutType
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.enums.PaginationType
import br.com.fenix.bilingualreader.model.enums.ScrollingType
import br.com.fenix.bilingualreader.model.enums.SpacingLayoutType
import br.com.fenix.bilingualreader.model.enums.TextSpeech
import br.com.fenix.bilingualreader.service.controller.WebInterface
import br.com.fenix.bilingualreader.service.japanese.Formatter
import br.com.fenix.bilingualreader.service.listener.TextSelectCallbackListener
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.parses.book.ImageParse
import br.com.fenix.bilingualreader.service.repository.BookAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.SharedData
import br.com.fenix.bilingualreader.service.repository.VocabularyRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.ColorUtil
import br.com.fenix.bilingualreader.util.helpers.ImageUtil
import br.com.fenix.bilingualreader.util.helpers.Telemetry
import br.com.fenix.bilingualreader.util.helpers.TextUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil
import br.com.fenix.bilingualreader.view.components.ImageGetter
import br.com.fenix.bilingualreader.view.components.book.TextViewAdapter
import br.com.fenix.bilingualreader.view.components.book.TextViewClickMovement
import br.com.fenix.bilingualreader.view.components.book.TextViewSelectCallback
import br.com.fenix.bilingualreader.view.ui.popup.PopupAnnotations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory


class BookReaderViewModel(var app: Application) : AndroidViewModel(app) {

    private var mPreferences: SharedPreferences = GeneralConsts.getSharedPreferences(app.applicationContext)
    private val mRepository: BookRepository = BookRepository(app.applicationContext)
    private val mVocabularyRepository = VocabularyRepository(app.applicationContext)
    private val mAnnotationRepository = BookAnnotationRepository(app.applicationContext)

    private val mLOGGER = LoggerFactory.getLogger(BookReaderViewModel::class.java)

    // --------------------------------------------------------- Book ---------------------------------------------------------
    private var mBook: MutableLiveData<Book?> = MutableLiveData(null)
    val book: LiveData<Book?> = mBook

    private var mConfiguration: MutableLiveData<BookConfiguration?> = MutableLiveData(null)
    val configuration: LiveData<BookConfiguration?> = mConfiguration

    private var mLanguage: MutableLiveData<Languages> = MutableLiveData(Languages.ENGLISH)
    val language: LiveData<Languages> = mLanguage

    private var mTTSVoice: MutableLiveData<TextSpeech> = MutableLiveData(TextSpeech.getDefault(false))
    val ttsVoice: LiveData<TextSpeech> = mTTSVoice

    private var mTTSSpeed: MutableLiveData<Float> = MutableLiveData(GeneralConsts.KEYS.READER.BOOK_READER_TTS_SPEED_DEFAULT)
    val ttsSpeed: LiveData<Float> = mTTSSpeed

    // --------------------------------------------------------- Fonts / Layout ---------------------------------------------------------
    private var mListFonts: MutableLiveData<MutableList<Pair<FontType, Boolean>>> = MutableLiveData(arrayListOf())
    val fonts: LiveData<MutableList<Pair<FontType, Boolean>>> = mListFonts

    private var mAlignmentType: MutableLiveData<AlignmentLayoutType> = MutableLiveData(AlignmentLayoutType.Justify)
    val alignmentType: LiveData<AlignmentLayoutType> = mAlignmentType

    private var mMarginType: MutableLiveData<MarginLayoutType> = MutableLiveData(MarginLayoutType.Small)
    val marginType: LiveData<MarginLayoutType> = mMarginType

    private var mSpacingType: MutableLiveData<SpacingLayoutType> = MutableLiveData(SpacingLayoutType.Small)
    val spacingType: LiveData<SpacingLayoutType> = mSpacingType

    private var mScrollingMode: MutableLiveData<ScrollingType> = MutableLiveData(ScrollingType.Pagination)
    val scrollingMode: LiveData<ScrollingType> = mScrollingMode

    private var mPaginationType: MutableLiveData<PaginationType> = MutableLiveData(PaginationType.Default)
    val paginationType: LiveData<PaginationType> = mPaginationType

    private var mFontType: MutableLiveData<FontType> = MutableLiveData(FontType.TimesNewRoman)
    val fontType: LiveData<FontType> = mFontType

    private var mFontSize: MutableLiveData<Float> = MutableLiveData(GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT)
    val fontSize: LiveData<Float> = mFontSize

    private var mFontCss: MutableLiveData<String> = MutableLiveData("")
    val fontCss: LiveData<String> = mFontCss

    private var mFontUpdate: MutableLiveData<String> = MutableLiveData("")
    val fontUpdate: LiveData<String> = mFontUpdate

    var history: History? = null

    private var mFontsLocation: String = FontType.getCssFont()
    private var mDefaultCss: String = ""
    private var isJapanese = false
    private var isJapaneseStyle = mPreferences.getBoolean(GeneralConsts.KEYS.READER.BOOK_FONT_JAPANESE_STYLE, false)
    private var isProcessJapaneseText = mPreferences.getBoolean(GeneralConsts.KEYS.READER.BOOK_PROCESS_JAPANESE_TEXT, true)
    private var isFurigana = mPreferences.getBoolean(GeneralConsts.KEYS.READER.BOOK_GENERATE_FURIGANA_ON_TEXT, true)
    private val mAnnotation = mutableListOf<BookAnnotation>()
    private var isDark : Boolean = false

    init {
        loadPreferences(false)
    }

    fun getDefaultCSS(): String {
        if (mDefaultCss.isEmpty())
            mDefaultCss = generateCSS()
        return mDefaultCss
    }

    private fun getFontColor(): String = if (isDark) "#ffffff" else "#000000"
    private fun getFontType(): String = fontType.value?.name ?: FontType.TimesNewRoman.name

    private fun getDiffer(): Float = if (isJapanese) (DocumentParse.BOOK_FONT_JAPANESE_SIZE_DIFFER * if (isFurigana) 1.5f else 1f ) else DocumentParse.BOOK_FONT_SIZE_DIFFER
    fun getFontSize(isBook: Boolean = false): Float = (if (isBook) fontSize.value!! + getDiffer() else fontSize.value!!)

    fun isJapaneseStyle() = isJapanese && isJapaneseStyle

    private fun generateCSS(): String {
        val fontColor = getFontColor()

        val fontType = getFontType()
        val fontSize = getFontSize().toString() + "dp"

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
        mConfiguration.value?.let { saveBookConfiguration(it) }
        return "<head>$meta$style</head>"
    }


    private fun fontUpdate() {
        val fontColor = getFontColor()

        val fontType = getFontType()

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

        mFontUpdate.value = fontColor + fontType + margin + spacing + alignment + isJapanese.toString() + isFurigana.toString()
        mConfiguration.value?.let { saveBookConfiguration(it) }
    }

    fun changeTextStyle(textView: TextView) {
        textView.setTextColor(ColorUtil.getColor(getFontColor()))
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, getFontSize())
        val font = if (isJapaneseStyle()) fontType.value!!.getFontRotate() else fontType.value!!.getFont()
        textView.typeface = ResourcesCompat.getFont(app.applicationContext, font)
        textView.rotation = if (isJapaneseStyle()) 90f else 0f

        val margin = when (marginType.value) {
            MarginLayoutType.Small -> 10
            MarginLayoutType.Medium -> 30
            MarginLayoutType.Big -> 50
            else -> 10
        }

        val alignment = alignmentType.value ?: AlignmentLayoutType.Justify
        val horizontalGravity = when {
            isJapaneseStyle() -> Gravity.START
            alignment == AlignmentLayoutType.Right -> Gravity.END
            alignment == AlignmentLayoutType.Center -> Gravity.CENTER_HORIZONTAL
            else -> Gravity.START // Left and Justify
        }

        val params: FrameLayout.LayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(margin, margin, margin, margin + 10)
        params.gravity = horizontalGravity
        textView.layoutParams = params

        val spacing = when (spacingType.value) {
            SpacingLayoutType.Small -> 0f
            SpacingLayoutType.Medium -> 15f
            SpacingLayoutType.Big -> 30f
            else -> 0f
        }
        textView.setLineSpacing(spacing, 1f)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            textView.justificationMode = if (!isJapaneseStyle() && alignment == AlignmentLayoutType.Justify)
                JUSTIFICATION_MODE_INTER_WORD
            else
                0
        }

        textView.gravity = horizontalGravity or Gravity.TOP
        textView.textAlignment = if (isJapaneseStyle())
            View.TEXT_ALIGNMENT_TEXT_START
        else {
            when (alignment) {
                AlignmentLayoutType.Right -> View.TEXT_ALIGNMENT_TEXT_END
                AlignmentLayoutType.Center -> View.TEXT_ALIGNMENT_CENTER
                else -> View.TEXT_ALIGNMENT_TEXT_START // Left and Justify
            }
        }

        textView.requestLayout()
    }

    fun changeScrolling(scrolling: ScrollingType) {
        mScrollingMode.value = scrolling
        mConfiguration.value?.let { saveBookConfiguration(it) }
    }

    fun changePagination(pagination: PaginationType) {
        mPaginationType.value = pagination
        mConfiguration.value?.let { saveBookConfiguration(it) }
    }

    fun changeLanguage(language: Languages, isLoad: Boolean = false) {
        mLanguage.value = language

        if (!isLoad) {
            mBook.value!!.language = language
            mRepository.update(mBook.value!!)
        }

        val isJapanese = language == Languages.JAPANESE
        if (isJapanese != this.isJapanese) {
            this.isJapanese = isJapanese

            val typeKey = if (isJapanese) GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_TYPE_JAPANESE else GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_TYPE_NORMAL
            val typeDefault = if (isJapanese) FontType.BabelStoneErjian1.toString() else FontType.TimesNewRoman.toString()
            loadFonts(isJapanese)
            setSelectFont(FontType.valueOf(mPreferences.getString(typeKey, typeDefault)!!))

            val voice = if (isJapanese) GeneralConsts.KEYS.READER.BOOK_READER_TTS_VOICE_JAPANESE else GeneralConsts.KEYS.READER.BOOK_READER_TTS_VOICE_NORMAL
            mTTSVoice.value = TextSpeech.valueOf(mPreferences.getString(voice, TextSpeech.getDefault(isJapanese).toString())!!)
            mTTSSpeed.value = mPreferences.getFloat(GeneralConsts.KEYS.READER.BOOK_READER_TTS_SPEED, GeneralConsts.KEYS.READER.BOOK_READER_TTS_SPEED_DEFAULT)
        }
    }

    fun changeTTSVoice(voice: TextSpeech, speed: Float) {
        mTTSVoice.value = voice
        mTTSSpeed.value = speed

        with(GeneralConsts.getSharedPreferences(app.applicationContext).edit()) {
            val key = if (isJapanese) GeneralConsts.KEYS.READER.BOOK_READER_TTS_VOICE_JAPANESE else GeneralConsts.KEYS.READER.BOOK_READER_TTS_VOICE_NORMAL
            this.putString(key, voice.toString())
            this.putFloat(GeneralConsts.KEYS.READER.BOOK_READER_TTS_SPEED, speed)
            this.commit()
        }
    }

    fun changeJapanese(process: Boolean, furigana: Boolean) {
        if (process == isProcessJapaneseText && furigana == isFurigana)
            return

        isProcessJapaneseText = process
        isFurigana = furigana
        fontUpdate()
    }

    fun loadConfiguration(book: Book?) {
        mBook.value = book

        changeLanguage(book?.language ?: Languages.ENGLISH, true)

        mConfiguration.value = if (book?.id != null) mRepository.findConfiguration(book.id!!) else null
        loadConfiguration(mConfiguration.value)

        if (book?.id != null) {
            if (mConfiguration.value == null)
                saveBookConfiguration(book.id!!)
            mVocabularyRepository.processVocabulary(app.applicationContext, book.id!!)
        }
        refreshAnnotations(book)
    }

    private fun loadPreferences(isJapanese: Boolean) {
        isDark = ThemeUtil.applyThemeMode(app.applicationContext)

        val voice = if (isJapanese) GeneralConsts.KEYS.READER.BOOK_READER_TTS_VOICE_JAPANESE else GeneralConsts.KEYS.READER.BOOK_READER_TTS_VOICE_NORMAL
        mTTSVoice.value = TextSpeech.valueOf(mPreferences.getString(voice, TextSpeech.getDefault(isJapanese).toString())!!)
        mTTSSpeed.value = mPreferences.getFloat(GeneralConsts.KEYS.READER.BOOK_READER_TTS_SPEED, GeneralConsts.KEYS.READER.BOOK_READER_TTS_SPEED_DEFAULT)

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

        mScrollingMode.value = ScrollingType.valueOf(
            mPreferences.getString(
                GeneralConsts.KEYS.READER.BOOK_PAGE_SCROLLING_MODE,
                ScrollingType.Pagination.toString()
            )!!
        )

        mPaginationType.value = PaginationType.valueOf(
            mPreferences.getString(
                GeneralConsts.KEYS.READER.BOOK_PAGE_PAGINATION_TYPE,
                PaginationType.Default.toString()
            )!!
        )

        if (ReaderConsts.READER.BOOK_WEB_VIEW_MODE)
            mDefaultCss = generateCSS()
        else
            fontUpdate()
    }

    fun loadConfiguration(configuration: BookConfiguration?) {
        if (configuration == null)
            loadPreferences(isJapanese)
        else {
            mAlignmentType.value = configuration.alignment
            mMarginType.value = configuration.margin
            mScrollingMode.value = configuration.scrolling
            mPaginationType.value = configuration.pagination
            mSpacingType.value = configuration.spacing
            mFontType.value = configuration.fontType
            mFontSize.value = configuration.fontSize
            if (ReaderConsts.READER.BOOK_WEB_VIEW_MODE)
                mDefaultCss = generateCSS()
            else
                fontUpdate()
        }
    }


    fun saveBookConfiguration(idBook: Long) {
        mConfiguration.value = mRepository.findConfiguration(idBook) ?: BookConfiguration(
            null,
            idBook,
            AlignmentLayoutType.Justify,
            MarginLayoutType.Small,
            SpacingLayoutType.Small,
            FontType.TimesNewRoman,
            GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT,
            ScrollingType.Pagination,
            PaginationType.Default
        )
        saveBookConfiguration(mConfiguration.value!!)
    }

    fun saveBookConfiguration(configuration: BookConfiguration) {
        configuration.alignment = alignmentType.value!!
        configuration.margin = marginType.value!!
        configuration.scrolling = scrollingMode.value!!
        configuration.pagination = paginationType.value!!
        configuration.spacing = spacingType.value!!
        configuration.fontType = fontType.value!!
        configuration.fontSize = fontSize.value!!

        if (configuration.id == null)
            mRepository.saveConfiguration(configuration)
        else
            mRepository.updateConfiguration(configuration)
    }

    fun loadFonts() = loadFonts(isJapanese)

    private fun loadFonts(isJapanese: Boolean) {
        val fonts = FontType.values().sortedWith(compareBy({ if (isJapanese) !it.isJapanese() else it.isJapanese() }, { it == FontType.TimesNewRoman }, { it.name }))
        mListFonts.value = fonts.map { Pair(it, it == mFontType.value) }.toMutableList()
    }

    fun getSelectedFontTypeIndex(): Int {
        val index = mListFonts.value?.indexOfFirst { it.second } ?: 0
        return if (index == -1) 0 else index
    }

    fun setSelectFont(font: FontType) {
        mFontType.value = font
        if (mListFonts.value != null)
            mListFonts.value = mListFonts.value!!.map { Pair(it.first, it.first == font) }.toMutableList()

        if (ReaderConsts.READER.BOOK_WEB_VIEW_MODE)
            mDefaultCss = generateCSS()
        else
            fontUpdate()
    }

    fun changeFontSize(value: Float) {
        mFontSize.value = value
        if (ReaderConsts.READER.BOOK_WEB_VIEW_MODE)
            mDefaultCss = generateCSS()
        else
            fontUpdate()
    }

    fun setSelectAlignment(alignment: AlignmentLayoutType) {
        mAlignmentType.value = alignment
        if (ReaderConsts.READER.BOOK_WEB_VIEW_MODE)
            mDefaultCss = generateCSS()
        else
            fontUpdate()
    }

    fun setSelectSpacing(spacing: SpacingLayoutType) {
        mSpacingType.value = spacing
        if (ReaderConsts.READER.BOOK_WEB_VIEW_MODE)
            mDefaultCss = generateCSS()
        else
            fontUpdate()
    }

    fun setSelectMargin(margin: MarginLayoutType) {
        mMarginType.value = margin
        if (ReaderConsts.READER.BOOK_WEB_VIEW_MODE)
            mDefaultCss = generateCSS()
        else
            fontUpdate()
    }

    @SuppressLint("JavascriptInterface")
    fun prepareHtml(parse: DocumentParse?, page: Int, web: WebView, listener: View.OnTouchListener? = null, javascript: WebInterface? = null) {
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
        web.settings.defaultFontSize = getFontSize().toInt()
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

    fun prepareHtml(context: Context, parse: DocumentParse?, page: Int, holder: TextViewAdapter.TextViewPagerHolder, listener: TextSelectCallbackListener?) {
        holder.pageMark.visibility = if (mAnnotation.any { it.page == page && it.markType == MarkType.PageMark }) View.VISIBLE else View.GONE

        // Abre a pagina nativa uma unica vez (antes abria duas: uma sem reciclar - vazamento -
        // e outra apenas para reciclar), reduzindo o custo de JNI por bind.
        val documentPage = parse?.getPage(page)
        var text = documentPage?.pageHTMLWithImages.orEmpty()

        if (text.contains("<image-begin>image"))
            text = text.replace("<image-begin>", "<img src=\"data:").replace("<image-end>", "\" />")

        val content = TextUtil.formatHtml(text).trim()
        holder.isOnlyImage = TextUtil.isOnlyImageOnHtml(content)

        if (holder.isOnlyImage) {
            try {
                val images = TextUtil.getImagesFromTag(text)
                var bitmaps = emptyList<Bitmap>()
                if (images.isNotEmpty()) {
                    bitmaps = images.mapNotNull {
                        val img = it.substringAfter(",").trim()
                        ImageUtil.decodeImageBase64(img)
                    }
                }
                if (bitmaps.isEmpty() && parse != null) {
                    val nativeBitmap = ImageParse(context).getPage(parse.path, page)
                    if (nativeBitmap != null) {
                        bitmaps = listOf(nativeBitmap)
                    }
                }
                if (bitmaps.isNotEmpty()) {
                    val alignment = alignmentType.value ?: AlignmentLayoutType.Justify
                    val combined = if (TextUtil.hasBrBetweenImages(text)) {
                        ImageUtil.combineImagesVertically(bitmaps, alignment)
                    } else {
                        ImageUtil.combineImagesHorizontally(bitmaps)
                    }
                    holder.imageView.setImageBitmap(combined)
                } else {
                    holder.imageView.setImageBitmap(null)
                }
            } catch (e: Exception) {
                mLOGGER.error("Error to generate image: " + e.message, e)
                holder.imageView.setImageBitmap(null)
                Telemetry.recordException(e, "Error to generate image: " + e.message)
            }
            holder.textView.linksClickable = false
            holder.textView.movementMethod = null
            holder.textView.customSelectionActionModeCallback = null
        } else {
            val html = "<body>${content}</body>"
            holder.imageView.setImageBitmap(null)
            var processed: Spanned = SpannableString("")
            try {
                processed = if (html.contains("<img"))
                    Html.fromHtml(
                        html,
                        Html.FROM_HTML_MODE_LEGACY,
                        ImageGetter(context, holder.textView),
                        null
                    )
                else
                    Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)

                processed = SpannableString(processed)

                if (isProcessJapaneseText && isJapanese)
                    Formatter.generateTextView(context, processed, isFurigana)

                val createSpanSelect = { annotation: BookAnnotation, _: Int, _: Int ->
                    val span = SpannableString(holder.textView.text)
                    createSpan(context, span, annotation, listener)
                    holder.textView.text = span
                }
                holder.textView.customSelectionActionModeCallback = TextViewSelectCallback(context, holder, page, createSpanSelect, listener)
                processed = prepareSpan(context, processed, page, listener)
                holder.textView.isClickable = true
                holder.textView.linksClickable = true
                holder.textView.setCustomMovement(TextViewClickMovement.getInstance(holder))
            } finally {
                holder.textView.text = processed
            }
        }
        documentPage?.recycle()
        holder.textView.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun prepareSpan(context: Context, span: SpannableString, page: Int, listener: TextSelectCallbackListener?): SpannableString {
        var spanned = span
        val marks = mAnnotation.filter { it.page == page && it.fontSize == mFontSize.value }
        if (marks.isNotEmpty()) {
            val process = SpannableString(spanned)
            var isSpan = false

            for (mark in marks) {
                if (mark.color == br.com.fenix.bilingualreader.model.enums.Color.None || mark.range[1] > span.length || mark.range[0] > span.length)
                    continue

                isSpan = true
                createSpan(context, process, mark, listener)
            }

            if (isSpan)
                spanned = process
        }
        return spanned
    }

    private fun createSpan(context: Context, span: SpannableString, annotation: BookAnnotation, listener: TextSelectCallbackListener?) {
        val delete = { delete: BookAnnotation ->
            mAnnotation.remove(delete)
            listener?.textSelectRemoveMark(annotation)
            true
        }
        val click = PopupAnnotations.generateClick(context, annotation, app.getColor(annotation.color.getColor()), delete) { alter ->
            if (alter)
                listener?.textSelectChangeMark(annotation)
        }

        val spannable = span.getSpans(annotation.range[0], annotation.range[1], ClickableSpan::class.java)
        if (spannable != null && spannable.isNotEmpty()) {
            for (i in spannable.indices)
                span.removeSpan(spannable[i])
        }

        span.setSpan(click, annotation.range[0], annotation.range[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }


    // --------------------------------------------------------- Book ---------------------------------------------------------

    fun update(book: Book) = mRepository.update(book)


    // --------------------------------------------------------- Book - Annottation ---------------------------------------------------------

    fun save(annotation: BookAnnotation) {
        if (annotation.id == null)
            annotation.id = mAnnotationRepository.save(annotation)
        else
            mAnnotationRepository.update(annotation)

        if (!mAnnotation.contains(annotation))
            mAnnotation.add(annotation)
    }

    fun delete(annotation: BookAnnotation) {
        if (annotation.id != null)
            mAnnotationRepository.delete(annotation)

        if (mAnnotation.contains(annotation))
            mAnnotation.remove(annotation)
    }

    fun refreshAnnotations(book: Book?) {
        mAnnotation.clear()
        if (book?.id != null)
            mAnnotation.addAll(findAnnotationByBook(book))
    }

    fun findAnnotationByBook(book: Book) = mAnnotationRepository.findByBook(book.id!!)

    fun findAnnotationByPage(book: Book, page: Int) = mAnnotationRepository.findByPage(book.id!!, page)


    // --------------------------------------------------------- Book - Chapters ---------------------------------------------------------
    var isLoadChapters = false
    var stopLoadChapters = false

    private fun createChapterThumbnailTextView(context: Context, renderWidth: Int, renderHeight: Int, fontScale: Float): TextView {
        val textView = TextView(context)
        changeTextStyle(textView)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, (getFontSize() * fontScale).coerceAtLeast(4f))
        textView.layoutParams.width = renderWidth
        textView.layoutParams.height = renderHeight
        return textView
    }

    private fun getChapterThumbnailRenderSize(): Triple<Int, Int, Float> {
        val metrics = Resources.getSystem().displayMetrics
        val renderWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            ReaderConsts.PAGE.PAGE_CHAPTER_LIST_WIDTH * ReaderConsts.PAGE.PAGE_CHAPTER_THUMBNAIL_SCALE,
            metrics
        ).toInt()
        val renderHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            ReaderConsts.PAGE.PAGE_CHAPTER_LIST_HEIGHT * ReaderConsts.PAGE.PAGE_CHAPTER_THUMBNAIL_SCALE,
            metrics
        ).toInt()
        val fontScale = (renderWidth.toFloat() / metrics.widthPixels).coerceIn(0.15f, 0.6f)
        return Triple(renderWidth, renderHeight, fontScale)
    }

    private fun loadImage(context: Context, parse: DocumentParse, page: Int, textView: TextView, imageMaxWidth: Int): Bitmap? {
        try {
            val documentPage = parse.getPage(page)
            var text = documentPage.pageHTMLWithImages.orEmpty()

            if (text.contains("<image-begin>image"))
                text = text.replace("<image-begin>", "<img src=\"data:").replace("<image-end>", "\" />")

            val content = TextUtil.formatHtml(text).trim()
            val isOnlyImage = TextUtil.isOnlyImageOnHtml(content)
            val bitmap: Bitmap

            if (!isOnlyImage) {
                val html = "<body>$content</body>"
                val processed = SpannableString(
                    if (html.contains("<img"))
                        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY, ImageGetter(context, textView, imageMaxWidth), null)
                    else
                        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
                )

                val marks = mAnnotation.filter { it.page == page }
                if (marks.isNotEmpty()) {
                    for (mark in marks) {
                        if (mark.color == br.com.fenix.bilingualreader.model.enums.Color.None || mark.range[1] > processed.length || mark.range[0] > processed.length)
                            continue
                        processed.setSpan(ForegroundColorSpan(context.getColor(mark.color.getColor())), mark.range[0], mark.range[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
                textView.text = processed
                documentPage.recycle()

                bitmap = Bitmap.createBitmap(textView.layoutParams.width, textView.layoutParams.height, Bitmap.Config.RGB_565)
                val canvas = Canvas(bitmap)
                canvas.drawColor(if (isDark) Color.BLACK else Color.WHITE)
                val measuredWidth = View.MeasureSpec.makeMeasureSpec(textView.layoutParams.width, View.MeasureSpec.EXACTLY)
                val measuredHeight = View.MeasureSpec.makeMeasureSpec(textView.layoutParams.height, View.MeasureSpec.EXACTLY)
                textView.measure(measuredWidth, measuredHeight)
                textView.layout(0, 0, textView.measuredWidth, textView.measuredHeight)
                textView.draw(canvas)
            } else {
                documentPage.recycle()
                val images = TextUtil.getImagesFromTag(text)
                var bitmaps = images.mapNotNull {
                    ImageUtil.decodeImageBase64(it.substringAfter(",").trim(), imageMaxWidth, imageMaxWidth)
                }
                if (bitmaps.isEmpty()) {
                    val nativeBitmap = ImageParse(context).getPage(parse.path, page, imageMaxWidth)
                    if (nativeBitmap != null) {
                        bitmaps = listOf(nativeBitmap)
                    }
                }
                bitmap = when {
                    bitmaps.isEmpty() -> return null
                    bitmaps.size == 1 -> bitmaps[0]
                    TextUtil.hasBrBetweenImages(text) -> ImageUtil.combineImagesVertically(
                        bitmaps,
                        alignmentType.value ?: AlignmentLayoutType.Justify
                    ) ?: bitmaps[0]
                    else -> ImageUtil.combineImagesHorizontally(bitmaps) ?: bitmaps[0]
                }
            }
            return bitmap
        } catch (e: Exception) {
            mLOGGER.error("Error to load image page: " + e.message, e)
            Telemetry.recordException(e, "Error to load image page: " + e.message)
        }
        return null
    }

    fun loadChapter(context: Context, parse: DocumentParse?, number: Int): Boolean {
        if (parse == null) {
            SharedData.clearChapters()
            return false
        }

        if (SharedData.isProcessed(parse)) {
            isLoadChapters = true
            SharedData.clearChapters()
            stopLoadChapters = false
            val list = arrayListOf<Chapters>()
            val chapters = parse.getChapters().map { Pair(it.value, it.key) }.sortedBy { it.first }
            val pages = parse.pageCount
            var title = Chapters("", 0, 0, 0f, true)

            for (i in 0 until pages) {
                if (stopLoadChapters)
                    break

                val activeChapter = chapters.filter { it.first <= i + 1 }.maxByOrNull { it.first }
                val activeTitle = activeChapter?.second ?: parse.bookTitle
                val activeChapterPage = activeChapter?.first ?: 0

                if (title.title != activeTitle) {
                    title = Chapters(activeTitle, i, activeChapterPage, list.size.toFloat(), true)
                    list.add(title)
                }

                list.add(Chapters(title.title, i, i + 1, 0f, false, isSelected = number == i + 1))
            }

            val (renderWidth, renderHeight, fontScale) = getChapterThumbnailRenderSize()
            val textView = createChapterThumbnailTextView(context, renderWidth, renderHeight, fontScale)

            val startIndex = list.indexOfFirst { !it.isTitle && it.page - 1 >= number }
                .let { if (it >= 0) it else list.indexOfLast { c -> !c.isTitle } }
            val orderedIndices = mutableListOf<Int>()
            if (startIndex >= 0) {
                for (i in startIndex until list.size)
                    if (!list[i].isTitle) orderedIndices.add(i)
                for (i in startIndex - 1 downTo 0)
                    if (!list[i].isTitle) orderedIndices.add(i)
            }

            SharedData.setChapters(parse, list)
            CoroutineScope(Dispatchers.IO).launch {
                val deferred = async {
                    for (index in orderedIndices) {
                        if (stopLoadChapters)
                            break

                        val chapter = list[index]
                        chapter.image = loadImage(context, parse, chapter.number, textView, renderWidth)
                        withContext(Dispatchers.Main) {
                            if (!stopLoadChapters)
                                SharedData.callListeners(index)
                        }
                    }
                }

                deferred.await()
                withContext(Dispatchers.Main) {
                    if (!stopLoadChapters)
                        SharedData.setChapters(parse, list.toList())
                }
                isLoadChapters = false
            }
        } else if (SharedData.isImageNull())
            refreshImageChapter(context, parse)

        return true
    }

    private fun refreshImageChapter(context: Context, parse: DocumentParse) {
        val (renderWidth, renderHeight, fontScale) = getChapterThumbnailRenderSize()
        val textView = createChapterThumbnailTextView(context, renderWidth, renderHeight, fontScale)
        val chapters = SharedData.chapters.value ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val deferred = async {
                for ((index, chapter) in chapters.withIndex()) {
                    if (stopLoadChapters)
                        break

                    if (chapter.isTitle || chapter.image != null)
                        continue

                    chapter.image = loadImage(context, parse, chapter.number, textView, renderWidth)
                    withContext(Dispatchers.Main) {
                        if (!stopLoadChapters)
                            SharedData.callListeners(index)
                    }
                }
            }

            deferred.await()
            isLoadChapters = false
        }
    }

}