package br.com.fenix.bilingualreader.view.components.book

import android.content.Context
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.text.Spannable
import android.text.SpannableString
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Speech
import br.com.fenix.bilingualreader.model.enums.AudioStatus
import br.com.fenix.bilingualreader.model.enums.ScrollingType
import br.com.fenix.bilingualreader.service.listener.ScrollChangeListener
import br.com.fenix.bilingualreader.service.listener.SelectionChangeListener
import br.com.fenix.bilingualreader.service.listener.TTSListener
import br.com.fenix.bilingualreader.service.listener.TextSelectCallbackListener
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.tts.TTSTextColorSpan
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.PopupUtil
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import br.com.fenix.bilingualreader.view.components.manga.ImageViewPage
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderViewModel
import org.slf4j.LoggerFactory
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


class TextViewPager(
    var context: Context, model: BookReaderViewModel, parse: DocumentParse?, listener: View.OnTouchListener? = null,
    textSelectCallback: TextSelectCallbackListener? = null
) : RecyclerView.Adapter<TextViewPager.TextViewPagerHolder>(), TTSListener {

    private val mLOGGER = LoggerFactory.getLogger(TextViewPager::class.java)

    private var mParse = parse
    private val mViewModel = model
    private val mListener = listener
    private val mTextSelectCallback = textSelectCallback
    private val mHolders = mutableMapOf<Int, TextViewPagerHolder>()
    private var mSpeech: Speech? = null
    private var mItems : Int = 0

    init {
        refreshSize()
    }

    fun refreshSize() {
        mItems = mParse?.getPageCount(mViewModel.getFontSize(isBook = true).toInt()) ?: 1
    }

    fun refreshLayout(type: ScrollingType) {
        for(holder in mHolders.entries)
            configureLayout(holder.value, type, holder.key)
    }

    fun clearParse() {
        mParse = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewPagerHolder {
        val inflater = LayoutInflater.from(parent.context)
        val holder = TextViewPagerHolder(inflater.inflate(R.layout.fragment_book_text_view_page, parent, false))
        mViewModel.changeTextStyle(holder.textView)
        holder.style = mViewModel.fontUpdate.value + mViewModel.fontSize.value

        if (!ReaderConsts.READER.BOOK_NATIVE_POPUP_MENU_SELECT) {
            holder.popupTextSelect.contentView = inflater.inflate(R.layout.popup_text_select, null)
            holder.popupTextSelect.width = FrameLayout.LayoutParams.WRAP_CONTENT
            holder.popupTextSelect.height = FrameLayout.LayoutParams.WRAP_CONTENT

            holder.scrollView.setScrollChangeListener(holder)
            holder.textView.setSelectionChangeListener(holder)
        }

        return holder
    }

    override fun getItemCount(): Int = mItems

    override fun onBindViewHolder(holder: TextViewPagerHolder, position: Int) {
        if (!ReaderConsts.READER.BOOK_NATIVE_POPUP_MENU_SELECT) {
            //Prevent poupup not dimiss in change page
            for (holder in mHolders.values)
                holder.popupTextSelect.dismiss()
        }

        mViewModel.prepareHtml(context, mParse, position, holder, mTextSelectCallback)

        val font = mViewModel.fontUpdate.value + mViewModel.fontSize.value
        if (font != holder.style) {
            holder.style = font
            mViewModel.changeTextStyle(holder.textView)
        }

        holder.textView.resetZoom()

        if (!holder.isOnlyImage)
            holder.textView.fixTextSelection()
        else
            holder.textView.setTextIsSelectable(false)

        if (mListener != null)
            holder.scrollView.setOnTouchListener(mListener)

        holder.scrollView.scrollY = 0
        holder.scrollView.parent.requestDisallowInterceptTouchEvent(false)
        holder.scrollView.setOnScrollChangeListener { _, _, scrollY, _, scrollYOld ->
            val isScrollUp = scrollY <= scrollYOld
            if ((isScrollUp && scrollY <= 10) || (!isScrollUp && (holder.textView.bottom - holder.textView.top) <= (holder.scrollView.height + scrollY + 10)))
                holder.scrollView.parent.requestDisallowInterceptTouchEvent(false)
            else
                holder.scrollView.parent.requestDisallowInterceptTouchEvent(true)
        }

        configureLayout(holder, mViewModel.scrollingType.value ?: ScrollingType.Pagination, position)

        holder.textView.setOnTouchListener(mListener)
        holder.imageView.setOnTouchListener(mListener)

        if (holder.isOnlyImage) {
            holder.textView.visibility = View.GONE
            holder.imageView.visibility = View.VISIBLE
        } else {
            holder.textView.visibility = View.VISIBLE
            holder.imageView.visibility = View.GONE
        }

        holder.scrollView.visibility = holder.textView.visibility

        mHolders[position] = holder
        if (mSpeech != null && mSpeech!!.page == position)
            drawLineSpeech(holder.textView, mSpeech!!)
    }

    private fun configureLayout(holder: TextViewPagerHolder, type: ScrollingType, position: Int) {
        when (type) {
            ScrollingType.Scrolling -> {
                holder.scrollView.overScrollMode = View.OVER_SCROLL_NEVER
                holder.root.layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT
                holder.root.layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT
                holder.scrollView.layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT
                holder.scrollView.layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT
            }
            else -> {
                holder.scrollView.overScrollMode = if (type == ScrollingType.PaginationVertical && position > 0 && position < (mItems-1)) View.OVER_SCROLL_NEVER else View.OVER_SCROLL_ALWAYS
                holder.root.layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT
                holder.root.layoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT
                holder.scrollView.layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT
                holder.scrollView.layoutParams.height = FrameLayout.LayoutParams.MATCH_PARENT
            }
        }
    }

    private fun TextView.fixTextSelection() {
        setTextIsSelectable(false)
        post { setTextIsSelectable(true) }
    }

    fun getHolder(position: Int): TextViewPagerHolder? = if (mHolders.containsKey(position)) mHolders[position] else null

    inner class TextViewPagerHolder(itemView: View) : RecyclerView.ViewHolder(itemView), ScrollChangeListener, SelectionChangeListener {
        val root: FrameLayout = itemView.findViewById(R.id.frame_reader_page_root)
        val pageMark: ImageView = itemView.findViewById(R.id.page_mark)
        val scrollView: NotifyingScrollView = itemView.findViewById(R.id.page_scroll_view)
        val textView: TextViewPage = itemView.findViewById(R.id.page_text_view)
        val imageView: ImageViewPage = itemView.findViewById(R.id.page_image_view)
        var isOnlyImage: Boolean = false
        var style: String = ""

        val popupTextSelect: PopupWindow = PopupWindow()
        private val mCurrentLocation = Point()
        private val mStartLocation = Point()
        private val mBounds = Rect()

        private val mDefaultWidth = -1
        private val mDefaultHeight = -1

        override fun onScrollChanged() {
            if (ReaderConsts.READER.BOOK_NATIVE_POPUP_MENU_SELECT)
                return

            if (popupTextSelect.isShowing) {
                val location = calculatePopupLocation()
                popupTextSelect.update(location.x, location.y, mDefaultWidth, mDefaultHeight)
            } else
                textView.clearFocus()
        }

        override fun isShowingPopup(): Boolean = popupTextSelect.isShowing

        override fun onTextSelected() {
            if (ReaderConsts.READER.BOOK_NATIVE_POPUP_MENU_SELECT)
                return

            val popupContent: View = popupTextSelect.contentView
            if (popupTextSelect.isShowing) {
                val location = calculatePopupLocation()
                popupTextSelect.update(location.x, location.y, mDefaultWidth, mDefaultHeight)
            } else {
                // Add the popup to the Window and position it relative to the selected text bounds
                PopupUtil.onGlobalLayout(textView) {
                    popupTextSelect.showAtLocation(textView, Gravity.TOP, 0, 0)
                    // Wait for the popup content to be laid out
                    PopupUtil.onGlobalLayout(popupContent) {
                        val cframe = Rect()
                        val cloc = IntArray(2)
                        popupContent.getLocationOnScreen(cloc)
                        popupContent.getLocalVisibleRect(mBounds)
                        popupContent.getWindowVisibleDisplayFrame(cframe)

                        val scrollY = (textView.parent as View).scrollY
                        val tloc = IntArray(2)
                        textView.getLocationInWindow(tloc)

                        val startX = cloc[0] + mBounds.centerX()
                        val startY = cloc[1] + mBounds.centerY() - (tloc[1] - cframe.top) - scrollY
                        mStartLocation.set(startX, startY)

                        val ploc: Point = calculatePopupLocation()
                        popupTextSelect.update(ploc.x, ploc.y, mDefaultWidth, mDefaultHeight)
                    }
                }
            }
        }

        override fun onTextUnselected() {
            if (!ReaderConsts.READER.BOOK_NATIVE_POPUP_MENU_SELECT)
                popupTextSelect.dismiss()
        }

        private fun calculatePopupLocation(): Point {
            val parent = textView.parent as ScrollView

            // Calculate the selection start and end offset
            val selStart: Int = textView.selectionStart
            val selEnd: Int = textView.selectionEnd
            val min = max(0, min(selStart, selEnd))
            val max = max(0, max(selStart, selEnd))

            // Calculate the selection bounds
            val selBounds = RectF()
            val selection = Path()
            textView.layout.getSelectionPath(min, max, selection)
            selection.computeBounds(selBounds, true)

            // Retrieve the center x/y of the popup content
            val cx = mStartLocation.x.toFloat()

            // Calculate the top and bottom offset of the popup relative to the selection bounds
            val popupHeight = mBounds.height()
            val textPadding: Int = textView.paddingLeft
            val topOffset = (selBounds.top - popupHeight)
            val btmOffset = (selBounds.bottom + popupHeight)

            // Calculate the x/y coordinates for the popup relative to the selection bounds
            val scrollY = parent.scrollY
            val x = (selBounds.centerX() + (textPadding - cx).toFloat()).roundToInt()
            val y = (if (selBounds.top - scrollY < popupHeight) btmOffset else topOffset).roundToInt()
            mCurrentLocation.set(x, y - scrollY)
            return mCurrentLocation
        }
    }

    private fun drawLineSpeech(textViewPage: TextViewPage, speech: Speech) {
        val start: Int = textViewPage.text.indexOf(speech.html)

        if (start < 0)
            return

        val span = SpannableString(textViewPage.text)

        val spannable = span.getSpans(0, span.length, TTSTextColorSpan::class.java)
        if (spannable != null && spannable.isNotEmpty()) {
            for (t in spannable.indices)
                span.removeSpan(spannable[t])
        }

        span.setSpan(
            TTSTextColorSpan(context.getColorFromAttr(R.attr.colorPrimary)),
            start, start + speech.html.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textViewPage.text = span
    }

    override fun statusTTS(status: AudioStatus) {}

    override fun readingLine(speech: Speech) {
        mSpeech = speech
        if (mHolders.containsKey(speech.page))
            drawLineSpeech(mHolders[speech.page]!!.textView, speech)
    }

    override fun changePageTTS(old: Int, new: Int) {
        if (mHolders.containsKey(old)){
            val textView = mHolders[old]!!.textView
            val span = SpannableString(textView.text)

            val spannable = span.getSpans(0, span.length, TTSTextColorSpan::class.java)
            if (spannable != null && spannable.isNotEmpty()) {
                for (t in spannable.indices)
                    span.removeSpan(spannable[t])
            }
            
            textView.text = span
        }
    }

    override fun stopTTS() {
        for (holder in mHolders.keys)
            notifyItemChanged(holder)

        mSpeech = null
    }

}