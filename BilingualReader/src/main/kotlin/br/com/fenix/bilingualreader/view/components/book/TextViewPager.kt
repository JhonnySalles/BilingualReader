package br.com.fenix.bilingualreader.view.components.book

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderViewModel


class TextViewPager(
    activity: Activity,
    context: Context,
    model: BookReaderViewModel,
    parse: DocumentParse?,
    listener: View.OnTouchListener? = null
) : RecyclerView.Adapter<TextViewPager.TextViewPagerHolder>() {

    private val mParse = parse
    private val mViewModel = model
    private val mListener = listener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewPagerHolder {
        return TextViewPagerHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.fragment_book_text_view_page, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return mParse?.getPageCount(mViewModel.mWebFontSize) ?: 1
    }

    override fun onBindViewHolder(holder: TextViewPagerHolder, position: Int) {
        mViewModel.prepareHtml(mParse, position, holder.textViewPage, mListener)
    }

    inner class TextViewPagerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewPage: TextViewPage = itemView.findViewById<View>(R.id.page_text_view) as TextViewPage
    }
}