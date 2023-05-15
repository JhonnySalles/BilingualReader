package br.com.fenix.bilingualreader.view.components.book

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.service.controller.WebInterface
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderViewModel

class WebViewPager(context: Context, model: BookReaderViewModel, parse: DocumentParse?, listener : View.OnTouchListener? = null) : RecyclerView.Adapter<WebViewPager.WebViewPagerHolder>() {

    private val mParse = parse
    private val mViewModel = model
    private val mListener = listener
    private val mInterface = WebInterface(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebViewPagerHolder {
        return WebViewPagerHolder(LayoutInflater.from(parent.context).inflate(R.layout.fragment_book_page, parent, false))
    }

    override fun getItemCount(): Int {
        return mParse?.getPageCount(mViewModel.mWebFontSize) ?: 1
    }

    override fun onBindViewHolder(holder: WebViewPagerHolder, position: Int) {
        mViewModel.prepareHtml(mParse, position, holder.webViewPage, mListener, mInterface)
    }

    inner class WebViewPagerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val webViewPage: WebViewPage = itemView.findViewById<View>(R.id.page_web_view) as WebViewPage
    }
}