package br.com.fenix.bilingualreader.view.adapter.book

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookSearch
import br.com.fenix.bilingualreader.service.listener.BookSearchListener


class BookSearchLineAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var mListener: BookSearchListener
    private var mBookMarkList: MutableList<BookSearch> = mutableListOf()

    companion object {
        private const val HEADER = 1
        private const val CONTENT = 0
    }

    override fun getItemViewType(position: Int): Int =
        if (mBookMarkList[position].isTitle) HEADER else CONTENT

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            HEADER -> {
                (holder as BookSearchHeaderViewHolder).bind(mBookMarkList[position])
            }
            else -> {
                (holder as BookSearchViewHolder).bind(mBookMarkList[position], position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> BookSearchHeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.line_card_divider_book_search, parent, false), mListener
            )
            else -> BookSearchViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.line_card_book_search, parent, false), mListener
            )
        }
    }

    override fun getItemCount(): Int {
        return mBookMarkList.size
    }

    fun updateList(list: MutableList<BookSearch>) {
        mBookMarkList = list
        notifyDataSetChanged()
    }

    fun attachListener(listener: BookSearchListener) {
        mListener = listener
    }

    fun notifyItemChanged(mark: BookSearch) {
        if (mBookMarkList.contains(mark))
            notifyItemChanged(mBookMarkList.indexOf(mark))
    }

}