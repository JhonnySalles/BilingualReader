package br.com.fenix.bilingualreader.view.ui.reader.book

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.text.LineBreaker.JUSTIFICATION_MODE_INTER_WORD
import android.os.Build
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.clearSpans
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.BookConfiguration
import br.com.fenix.bilingualreader.model.entity.History
import br.com.fenix.bilingualreader.model.enums.AlignmentLayoutType
import br.com.fenix.bilingualreader.model.enums.FontType
import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.MarginLayoutType
import br.com.fenix.bilingualreader.model.enums.ScrollingType
import br.com.fenix.bilingualreader.model.enums.SpacingLayoutType
import br.com.fenix.bilingualreader.service.controller.WebInterface
import br.com.fenix.bilingualreader.service.japanese.Formatter
import br.com.fenix.bilingualreader.service.listener.TextSelectCallbackListener
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.repository.BookAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.BookRepository
import br.com.fenix.bilingualreader.service.repository.VocabularyRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.ColorUtil
import br.com.fenix.bilingualreader.util.helpers.TextUtil
import br.com.fenix.bilingualreader.view.components.ImageGetter
import br.com.fenix.bilingualreader.view.components.book.TextViewPage
import br.com.fenix.bilingualreader.view.components.book.TextViewPager
import br.com.fenix.bilingualreader.view.components.book.TextViewSelectCallback
import br.com.fenix.bilingualreader.view.ui.popup.PopupAnnotations
import org.slf4j.LoggerFactory


class BookReaderViewModel(var app: Application) : AndroidViewModel(app) {

    private var mPreferences: SharedPreferences = GeneralConsts.getSharedPreferences(app.applicationContext)
    private val mRepository: BookRepository = BookRepository(app.applicationContext)
    private val mVocabularyRepository = VocabularyRepository(app.applicationContext)
    private val mAnnotationRepository = BookAnnotationRepository(app.applicationContext)

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

    private var mFontUpdate: MutableLiveData<String> = MutableLiveData("")
    val fontUpdate: LiveData<String> = mFontUpdate

    var history: History? = null

    private var mFontsLocation: String = FontType.getCssFont()
    private var mDefaultCss: String = ""
    var isJapanese = true
    private var isProcessJapaneseText = mPreferences.getBoolean(GeneralConsts.KEYS.READER.BOOK_PROCESS_JAPANESE_TEXT, true)
    private var isFurigana = mPreferences.getBoolean(GeneralConsts.KEYS.READER.BOOK_GENERATE_FURIGANA_ON_TEXT, true)
    private var mConfiguration: BookConfiguration? = null
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
    fun getFontSize(isBook: Boolean = false): Float = (if (isBook) fontSize.value!! + DocumentParse.BOOK_FONT_SIZE_DIFFER else fontSize.value!!)

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
        mConfiguration?.let { saveBookConfiguration(it) }
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

        mFontUpdate.value = fontColor + fontType + margin + spacing + alignment
        mConfiguration?.let { saveBookConfiguration(it) }
    }

    fun changeTextStyle(textView: TextViewPage) {
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
        params.gravity = Gravity.CENTER_VERTICAL
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

    fun loadConfiguration(book: Book?) {
        isJapanese = book?.language == Languages.JAPANESE
        mConfiguration = if (book?.id != null) mRepository.findConfiguration(book.id!!) else null
        loadConfiguration(mConfiguration)

        if (book?.id != null) {
            if (mConfiguration == null)
                saveBookConfiguration(book.id!!)
            mVocabularyRepository.processVocabulary(app.applicationContext, book.id!!)
        }
        refreshAnnotations(book)
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
        mConfiguration = mRepository.findConfiguration(idBook) ?: BookConfiguration(
            null,
            idBook,
            AlignmentLayoutType.Justify,
            MarginLayoutType.Small,
            SpacingLayoutType.Small,
            FontType.TimesNewRoman,
            GeneralConsts.KEYS.READER.BOOK_PAGE_FONT_SIZE_DEFAULT,
            ScrollingType.Pagination
        )
        saveBookConfiguration(mConfiguration!!)
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

    fun refreshAnnotations(book: Book?) {
        mAnnotation.clear()
        if (book?.id != null)
            mAnnotation.addAll(findAnnotationByBook(book))
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

    fun prepareHtml(
        context: Context,
        parse: DocumentParse?,
        page: Int,
        holder: TextViewPager.TextViewPagerHolder,
        listener: TextSelectCallbackListener?
    ) {
        var text = parse?.getPage(page)?.pageHTMLWithImages.orEmpty()

        if (text.contains("<image-begin>image"))
            text = text.replace("<image-begin>", "<img src=\"data:")
                .replace("<image-end>", "\" />")

        text = TextUtil.formatHtml(text)

        if (isProcessJapaneseText && isJapanese)
            text = Formatter.generateHtmlText(text, isFurigana)

        val html = "<body>$text</body>"

        parse?.getPage(page)?.recycle()

        holder.textView.isOnlyImage = html.contains("<img") && (text.endsWith(" /><br/>") || text.endsWith(" />"))

        holder.textView.text = if (html.contains("<img"))
            Html.fromHtml(
                html,
                Html.FROM_HTML_MODE_LEGACY,
                ImageGetter(context, holder.textView, holder.textView.isOnlyImage),
                null
            )
        else
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)


        if (!holder.textView.isOnlyImage) {
            val changeSelect = { annotation: BookAnnotation, start: Int, end: Int ->
                val span = SpannableString(holder.textView.text)
                createSpan(context, holder.textView, page, annotation, span, start, end)
                holder.textView.text = span
            }
            holder.textView.customSelectionActionModeCallback = TextViewSelectCallback(context, holder.textView, page, changeSelect, listener)
            prepareSpan(context, holder.textView, page)
            holder.textView.movementMethod = LinkMovementMethod.getInstance()
            holder.textView.isClickable = true
            holder.textView.linksClickable = true
        } else {
            holder.textView.linksClickable = false
            holder.textView.movementMethod = null
        }

        holder.textView.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun prepareSpan(context: Context, textView: TextViewPage, page: Int) {
        val marks = mAnnotation.filter { it.page == page }
        if (marks.isNotEmpty()) {
            var isSpan = false
            val span = SpannableString(textView.text)
            span.clearSpans()

            for (mark in marks) {
                val i: Int = textView.text.indexOf(mark.text)

                if (i < 0)
                    continue

                isSpan = true
                createSpan(context, textView, page, mark, span, i, i + mark.text.length)
            }

            if (isSpan)
                textView.text = span
        }
    }

    private fun createSpan(
        context: Context,
        textView: TextViewPage,
        page: Int,
        annotation: BookAnnotation,
        span: SpannableString,
        start: Int,
        end: Int
    ) {
        var click: ClickableSpan? = null
        val delete = { delete: BookAnnotation ->
            mAnnotation.remove(delete)
            prepareSpan(context, textView, page)
        }
        click = PopupAnnotations.generateClick(context, annotation, app.getColor(annotation.color.getColor()), delete) { alter ->
            if (alter)
                prepareSpan(context, textView, page)
        }
        span.setSpan(click, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }


    // --------------------------------------------------------- Book ---------------------------------------------------------

    fun update(book: Book) = mRepository.update(book)


    // --------------------------------------------------------- Book - Annottation ---------------------------------------------------------

    fun save(annotation: BookAnnotation) {
        if (annotation.id == null)
            mAnnotationRepository.save(annotation)
        else
            mAnnotationRepository.update(annotation)
    }

    fun delete(annotation: BookAnnotation) {
        if (annotation.id != null)
            mAnnotationRepository.delete(annotation)
    }

    fun findAnnotationByBook(book: Book) = mAnnotationRepository.findByBook(book.id!!)

    fun findAnnotationByPage(book: Book, page: Int) = mAnnotationRepository.findByPage(book.id!!, page)

}