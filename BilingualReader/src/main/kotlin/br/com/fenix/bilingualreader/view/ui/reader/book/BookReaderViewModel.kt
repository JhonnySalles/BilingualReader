package br.com.fenix.bilingualreader.view.ui.reader.book

import android.R.attr.height
import android.R.attr.width
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.text.LineBreaker.JUSTIFICATION_MODE_INTER_WORD
import android.os.Build
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.BookConfiguration
import br.com.fenix.bilingualreader.model.entity.Chapters
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.enums.AlignmentLayoutType
import br.com.fenix.bilingualreader.model.enums.FontType
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.MarginLayoutType
import br.com.fenix.bilingualreader.model.enums.ScrollingType
import br.com.fenix.bilingualreader.model.enums.SpacingLayoutType
import br.com.fenix.bilingualreader.model.enums.TextSpeech
import br.com.fenix.bilingualreader.service.controller.WebInterface
import br.com.fenix.bilingualreader.service.japanese.Formatter
import br.com.fenix.bilingualreader.service.listener.TextSelectCallbackListener
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.repository.BookAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.SharedData
import br.com.fenix.bilingualreader.service.repository.VocabularyRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.ColorUtil
import br.com.fenix.bilingualreader.util.helpers.TextUtil
import br.com.fenix.bilingualreader.view.components.ImageGetter
import br.com.fenix.bilingualreader.view.components.book.TextViewClickMovement
import br.com.fenix.bilingualreader.view.components.book.TextViewPager
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

    private var mTTSVoice: MutableLiveData<TextSpeech> = MutableLiveData(TextSpeech.getDefault())
    val ttsVoice: LiveData<TextSpeech> = mTTSVoice

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

    private var mFontUpdate: MutableLiveData<String> = MutableLiveData("")
    val fontUpdate: LiveData<String> = mFontUpdate

    var history: History? = null

    private var mFontsLocation: String = FontType.getCssFont()
    private var mDefaultCss: String = ""
    var isJapanese = false
    private var isProcessJapaneseText = mPreferences.getBoolean(GeneralConsts.KEYS.READER.BOOK_PROCESS_JAPANESE_TEXT, true)
    private var isFurigana = mPreferences.getBoolean(GeneralConsts.KEYS.READER.BOOK_GENERATE_FURIGANA_ON_TEXT, true)
    private val mAnnotation = mutableListOf<BookAnnotation>()

    init {
        loadPreferences(false)
    }

    fun getDefaultCSS(): String {
        if (mDefaultCss.isEmpty())
            mDefaultCss = generateCSS()
        return mDefaultCss
    }

    fun getFontColor(): String = if (app.resources.getBoolean(R.bool.isNight)) "#ffffff" else "#000000"
    fun getFontType(): String = fontType.value?.name ?: FontType.TimesNewRoman.name

    private fun getDiffer(): Float = if (isJapanese) DocumentParse.BOOK_FONT_JAPANESE_SIZE_DIFFER else DocumentParse.BOOK_FONT_SIZE_DIFFER
    fun getFontSize(isBook: Boolean = false): Float = (if (isBook) fontSize.value!! + getDiffer() else fontSize.value!!)

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

        val meta =
            "<meta name=\"viewport\" content=\"height=device-height, width=device-width, initial-scale=1, maximum-scale=2, user-scalable=yes\" >"
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
        textView.typeface = ResourcesCompat.getFont(app.applicationContext, fontType.value!!.getFont())

        val margin = when (marginType.value) {
            MarginLayoutType.Small -> 10
            MarginLayoutType.Medium -> 20
            MarginLayoutType.Big -> 30
            else -> 10
        }

        val params: FrameLayout.LayoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(margin, margin, margin, margin)
        params.gravity = if (isJapanese && isFurigana) Gravity.CENTER_HORIZONTAL else Gravity.CENTER
        textView.layoutParams = params

        val spacing = when (spacingType.value) {
            SpacingLayoutType.Small -> 0f
            SpacingLayoutType.Medium -> 15f
            SpacingLayoutType.Big -> 30f
            else -> 0f
        }
        textView.setLineSpacing(spacing, 1f)

        textView.textAlignment = when (alignmentType.value) {
            AlignmentLayoutType.Justify -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    textView.justificationMode = JUSTIFICATION_MODE_INTER_WORD

                View.TEXT_ALIGNMENT_INHERIT
            }

            AlignmentLayoutType.Right -> View.TEXT_ALIGNMENT_TEXT_END
            AlignmentLayoutType.Left -> View.TEXT_ALIGNMENT_TEXT_START
            AlignmentLayoutType.Center -> View.TEXT_ALIGNMENT_CENTER
            else -> View.TEXT_ALIGNMENT_TEXT_START
        }
    }

    fun changeScrolling(scrolling: ScrollingType) {
        mScrollingType.value = scrolling
        mConfiguration.value?.let { saveBookConfiguration(it) }
    }

    fun changeLanguage(language: Languages) {
        mLanguage.value = language

        val isJapanese = language == Languages.JAPANESE
        if (isJapanese != this.isJapanese) {
            this.isJapanese = isJapanese

            val typeKey = if (isJapanese) GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_TYPE_JAPANESE else GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_TYPE_NORMAL
            val typeDefault = if (isJapanese) FontType.BabelStoneErjian1.toString() else FontType.TimesNewRoman.toString()
            loadFonts(isJapanese)
            setSelectFont(FontType.valueOf(mPreferences.getString(typeKey, typeDefault)!!))
        }
    }

    fun changeTTSVoice(textSpeech: TextSpeech) {
        mTTSVoice.value = textSpeech

        with(GeneralConsts.getSharedPreferences(app.applicationContext).edit()) {
            this.putString(
                GeneralConsts.KEYS.READER.BOOK_READER_TTS,
                textSpeech.toString()
            )
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

        changeLanguage(book?.language ?: Languages.ENGLISH)

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
        mTTSVoice.value = TextSpeech.valueOf(mPreferences.getString(GeneralConsts.KEYS.READER.BOOK_READER_TTS, TextSpeech.getDefault().toString())!!)

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

        mScrollingType.value = ScrollingType.valueOf(
            mPreferences.getString(
                GeneralConsts.KEYS.READER.BOOK_PAGE_SCROLLING_MODE,
                ScrollingType.Pagination.toString()
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
            mScrollingType.value = configuration.scrolling
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
            ScrollingType.Pagination
        )
        saveBookConfiguration(mConfiguration.value!!)
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

    fun loadFonts() = loadFonts(isJapanese)

    private fun loadFonts(isJapanese: Boolean) {
        val fonts = FontType.values().sortedWith(compareBy({ if (isJapanese) it.isJapanese() else !it.isJapanese() }, { it == FontType.TimesNewRoman }, { it.name }))
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

    fun prepareHtml(context: Context, parse: DocumentParse?, page: Int, holder: TextViewPager.TextViewPagerHolder, listener: TextSelectCallbackListener?) {
        var text = parse?.getPage(page)?.pageHTMLWithImages.orEmpty()

        if (text.contains("<image-begin>image"))
            text = text.replace("<image-begin>", "<img src=\"data:").replace("<image-end>", "\" />")


        val html = "<body>${TextUtil.formatHtml(text)}</body>"

        holder.textView.isOnlyImage = html.contains("<img") && (text.endsWith(" /><br/>") || text.endsWith(" />"))

        var processed: Spanned = SpannableString("")
        try {
            processed = if (html.contains("<img"))
                Html.fromHtml(
                    html,
                    Html.FROM_HTML_MODE_LEGACY,
                    ImageGetter(context, holder.textView, holder.textView.isOnlyImage),
                    null
                )
            else
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)

            processed = SpannableString(processed)

            if (!holder.textView.isOnlyImage && isProcessJapaneseText && isJapanese)
                Formatter.generateTextView(context, processed, isFurigana)

            if (!holder.textView.isOnlyImage) {
                val createSpanSelect = { annotation: BookAnnotation, start: Int, end: Int ->
                    val span = SpannableString(holder.textView.text)
                    createSpan(context, span, annotation, listener)
                    holder.textView.text = span
                }
                holder.textView.customSelectionActionModeCallback = TextViewSelectCallback(context, holder, page, createSpanSelect, listener)
                processed = prepareSpan(context, processed, page, listener)
                holder.textView.isClickable = true
                holder.textView.linksClickable = true
                holder.textView.setCustomMovement(TextViewClickMovement.getInstance())
            } else {
                holder.textView.linksClickable = false
                holder.textView.movementMethod = null
                holder.textView.customSelectionActionModeCallback = null
            }

        } finally {
            holder.textView.text = processed
        }
        parse?.getPage(page)?.recycle()
        holder.textView.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun prepareSpan(context: Context, span: SpannableString, page: Int, listener: TextSelectCallbackListener?): SpannableString {
        var spanned = span
        val marks = mAnnotation.filter { it.page == page }
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
        var click: ClickableSpan? = null
        val delete = { delete: BookAnnotation ->
            mAnnotation.remove(delete)
            listener?.textSelectRemoveMark(annotation)
            true
        }
        click = PopupAnnotations.generateClick(context, annotation, app.getColor(annotation.color.getColor()), delete) { alter ->
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
    private fun loadImage(context: Context, parse: DocumentParse, page: Int, textView : TextView) : Bitmap? {
        try {
            var text = parse.getPage(page).pageHTMLWithImages.orEmpty()

            if (text.contains("<image-begin>image"))
                text = text.replace("<image-begin>", "<img src=\"data:").replace("<image-end>", "\" />")

            val html = "<body>${TextUtil.formatHtml(text)}</body>"

            val isOnlyImage = html.contains("<img") && (text.endsWith(" /><br/>") || text.endsWith(" />"))

            val bitmap :Bitmap

            if (!isOnlyImage) {
                val processed = SpannableString(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY))

                val marks = mAnnotation.filter { it.page == page }
                if (marks.isNotEmpty()) {
                    for (mark in marks) {
                        if (mark.color == br.com.fenix.bilingualreader.model.enums.Color.None || mark.range[1] > processed.length || mark.range[0] > processed.length)
                            continue
                        processed.setSpan(ForegroundColorSpan(context.getColor(mark.color.getColor())), mark.range[0], mark.range[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
                textView.text = processed
                parse.getPage(page).recycle()

                bitmap = Bitmap.createBitmap(textView.layoutParams.width, textView.layoutParams.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val measuredWidth = View.MeasureSpec.makeMeasureSpec(textView.layoutParams.width, View.MeasureSpec.EXACTLY)
                val measuredHeight = View.MeasureSpec.makeMeasureSpec(textView.layoutParams.height, View.MeasureSpec.EXACTLY)
                textView.measure(measuredWidth, measuredHeight)
                textView.layout(0, 0, textView.measuredWidth, textView.measuredHeight)

                textView.draw(canvas)
            } else {
                val image = text.substringAfter("<img").substringBefore("/>")
                val bytes: ByteArray = Base64.decode(image.substringAfter(",").trim(), Base64.DEFAULT)
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            return bitmap
        } catch (e: Exception) {
            mLOGGER.error("Error to load image page", e)
        }
        return null
    }

    fun loadChapter(context: Context, parse: DocumentParse?, number: Int): Boolean {
        if (parse == null) {
            SharedData.clearChapters()
            return false
        }

        if (SharedData.isProcessed(parse)) {
            val list = arrayListOf<Chapters>()
            val chapters = parse.getChapters()
            val pages = parse.pageCount
            var title: Chapters
            var c = 0
            var p = chapters[c].first
            title = Chapters(chapters[c].second, 0, chapters[c].first, c.toFloat(), true)
            list.add(title)

            for (i in 0 until pages) {
                if (i >= p && c < chapters.size - 1) {
                    c++
                    p = chapters[c].first
                    title = Chapters(chapters[c].second, 0, chapters[c].first, c.toFloat(), true)
                    list.add(title)
                }

                list.add(Chapters(chapters[c].second, i, i+1, 0f, false, isSelected = number == i+1))
            }

            val metrics = DisplayMetrics()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getMetrics(metrics)

            val textView = TextView(context)
            changeTextStyle(textView)
            textView.layoutParams.width = metrics.widthPixels
            textView.layoutParams.height = metrics.heightPixels

            SharedData.setChapters(parse, list)
            CoroutineScope(Dispatchers.IO).launch {
                val deferred = async {
                    for (chapter in list) {
                        if (chapter.isTitle)
                            continue

                        chapter.image = loadImage(context, parse, chapter.number, textView)
                        withContext(Dispatchers.Main) { SharedData.callListeners(chapter.number) }
                    }
                }

                deferred.await()
                withContext(Dispatchers.Main) {
                    SharedData.setChapters(parse, list.toList())
                }
            }
        }

        return true
    }

}