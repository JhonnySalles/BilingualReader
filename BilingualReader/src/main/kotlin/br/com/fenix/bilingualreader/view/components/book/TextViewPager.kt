package br.com.fenix.bilingualreader.view.components.book

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.util.helpers.ColorUtil
import br.com.fenix.bilingualreader.view.ui.reader.book.BookReaderViewModel


class TextViewPager(
    var context: Context,
    model: BookReaderViewModel,
    parse: DocumentParse?,
    listener: View.OnTouchListener? = null
) : RecyclerView.Adapter<TextViewPager.TextViewPagerHolder>() {

    private val mParse = parse
    private val mViewModel = model
    private val mListener = listener
    private val mPages = mParse?.getPageCount(mViewModel.getFontSize(isBook = true)) ?: 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewPagerHolder {
        val holder =  TextViewPagerHolder(LayoutInflater.from(parent.context).inflate(R.layout.fragment_book_text_view_page, parent, false))
        mViewModel.changeTextStyle(holder.textView)
        return holder
    }

    override fun getItemCount(): Int = mParse?.getPageCount(mViewModel.getFontSize(isBook = true)) ?: 1

    override fun onBindViewHolder(holder: TextViewPagerHolder, position: Int) {
        mViewModel.prepareHtml(mParse, position, holder, mListener)
    }

    inner class TextViewPagerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val background = itemView.findViewById<View>(R.id.page_back_view)
        val textView = itemView.findViewById<TextViewPage>(R.id.page_text_view)
    }

}