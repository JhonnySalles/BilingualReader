package br.com.fenix.bilingualreader.view.components.book

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnScrollChangedListener
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.text.clearSpans
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Speech
import br.com.fenix.bilingualreader.model.enums.AudioStatus
import br.com.fenix.bilingualreader.service.listener.TTSListener
import br.com.fenix.bilingualreader.service.listener.TextSelectCallbackListener
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderViewModel
import org.slf4j.LoggerFactory


class TextViewPager(
    var context: Context, model: BookReaderViewModel, parse: DocumentParse?, listener: View.OnTouchListener? = null,
    textSelectCallback: TextSelectCallbackListener? = null
) : RecyclerView.Adapter<TextViewPager.TextViewPagerHolder>(), TTSListener {

    private val mLOGGER = LoggerFactory.getLogger(TextViewPager::class.java)

    private val mParse = parse
    private val mViewModel = model
    private val mListener = listener
    private val mTextSelectCallback = textSelectCallback
    private val mHolders = mutableMapOf<Int, TextViewPagerHolder>()
    private var mSpeech: Speech? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewPagerHolder {
        val holder = TextViewPagerHolder(LayoutInflater.from(parent.context).inflate(R.layout.fragment_book_text_view_page, parent, false))
        mViewModel.changeTextStyle(holder.textView)
        holder.style = mViewModel.fontUpdate.value + mViewModel.fontSize.value
        return holder
    }

    override fun getItemCount(): Int = mParse?.getPageCount(mViewModel.getFontSize(isBook = true).toInt()) ?: 1

    override fun onBindViewHolder(holder: TextViewPagerHolder, position: Int) {
        mViewModel.prepareHtml(context, mParse, position, holder, mTextSelectCallback)

        val font = mViewModel.fontUpdate.value + mViewModel.fontSize.value
        if (font != holder.style) {
            holder.style = font
            mViewModel.changeTextStyle(holder.textView)
        }

        holder.textView.resetZoom()

        if (!holder.textView.isOnlyImage)
            holder.textView.fixTextSelection()
        else
            holder.textView.setTextIsSelectable(false)

        if (mListener != null)
            holder.scrollView.setOnTouchListener(mListener)

        holder.scrollView.parent.requestDisallowInterceptTouchEvent(false)
        holder.scrollView.viewTreeObserver.addOnScrollChangedListener(OnScrollChangedListener {
            if (holder.scrollView.getChildAt(0).bottom <= (holder.scrollView.height + holder.scrollView.scrollY) || holder.scrollView.scrollY == 0)
                holder.textView.parent.requestDisallowInterceptTouchEvent(false)
            else
                holder.textView.parent.requestDisallowInterceptTouchEvent(true)
        })

        holder.textView.setOnTouchListener { view, motionEvent ->
            view.performClick()
            mListener?.onTouch(view, motionEvent)
            false
        }

        mHolders[position] = holder
        if (mSpeech != null && mSpeech!!.page == position)
            drawLineSpeech(holder.textView, mSpeech!!)
    }

    private fun TextView.fixTextSelection() {
        setTextIsSelectable(false)
        post { setTextIsSelectable(true) }
    }

    inner class TextViewPagerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val scrollView = itemView.findViewById<ScrollView>(R.id.page_scroll_view)
        val textView = itemView.findViewById<TextViewPage>(R.id.page_text_view)
        var style: String = ""
    }

    private fun drawLineSpeech(textViewPage: TextViewPage, speech: Speech) {
        val i: Int = textViewPage.text.indexOf(speech.text)

        if (i < 0)
            return

        val span = SpannableString(textViewPage.text)
        span.clearSpans()
        span.setSpan(
            ForegroundColorSpan(context.getColorFromAttr(R.attr.colorOnSurfaceVariant)),
            i, i + speech.text.length,
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
        notifyItemChanged(old)
    }

    override fun stopTTS() {
        for (holder in mHolders.keys)
            notifyItemChanged(holder)

        mSpeech = null
    }

}