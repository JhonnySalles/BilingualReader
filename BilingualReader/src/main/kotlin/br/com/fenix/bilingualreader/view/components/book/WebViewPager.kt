package br.com.fenix.bilingualreader.view.components.book

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Speech
import br.com.fenix.bilingualreader.model.enums.AudioStatus
import br.com.fenix.bilingualreader.service.controller.WebInterface
import br.com.fenix.bilingualreader.service.functions.AutoScroll
import br.com.fenix.bilingualreader.service.listener.TTSListener
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderViewModel


class WebViewPager(
    activity: Activity, context: Context, model: BookReaderViewModel, parse: DocumentParse?, listener: View.OnTouchListener? = null
) : RecyclerView.Adapter<WebViewPager.WebViewPagerHolder>(), TTSListener, AutoScroll {

    private val mParse = parse
    private val mViewModel = model
    private val mListener = listener
    private val mInterface = WebInterface(activity, context)
    private var mPages = mParse?.getPageCount(mViewModel.getFontSize(isBook = true).toInt()) ?: 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebViewPagerHolder {
        return WebViewPagerHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.fragment_book_text_view_page, parent, false)
        )
    }

    override fun getItemCount(): Int = mPages

    override fun onBindViewHolder(holder: WebViewPagerHolder, position: Int) {
        mViewModel.prepareHtml(mParse, position, holder.webViewPage, mListener, mInterface)
    }

    fun changePages() {
        mPages = mParse?.getPageCount(mViewModel.getFontSize(isBook = true).toInt()) ?: 1
        notifyDataSetChanged()
    }

    inner class WebViewPagerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val webViewPage: WebViewPage = itemView.findViewById<View>(R.id.page_web_view) as WebViewPage
    }

    override fun statusTTS(status: AudioStatus) {
        TODO("Not yet implemented")
    }

    override fun readingLine(line: Speech) {
        TODO("Not yet implemented")
    }

    override fun changePageTTS(old: Int, new: Int) {
        TODO("Not yet implemented")
    }

    override fun stopTTS() {
        TODO("Not yet implemented")
    }

    override fun isVisible(): Boolean = true

    override fun autoScroll(isBack: Boolean): Boolean {
        TODO("Not yet implemented")
    }
}