package br.com.fenix.bilingualreader.view.adapter.book_mark

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookMark
import br.com.fenix.bilingualreader.service.listener.BookMarkListener


class BookMarkLineAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var mListener: BookMarkListener
    private var mBookMarkList: ArrayList<BookMark> = arrayListOf()

    companion object {
        private const val HEADER = 1
        private const val CONTENT = 0
    }

    override fun getItemViewType(position: Int): Int =
        if (mBookMarkList[position].id == null) HEADER else CONTENT

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            HEADER -> {
                (holder as BookMarkHeaderViewHolder).bind(mBookMarkList[position], position == 0)
            }
            else -> {
                (holder as BookMarkViewHolder).bind(mBookMarkList[position], position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> BookMarkHeaderViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.line_card_divider_book_mark, parent, false), mListener
            )
            else -> BookMarkViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.line_card_book_mark, parent, false), mListener
            )
        }
    }

    override fun getItemCount(): Int {
        return mBookMarkList.size
    }

    fun updateList(list: ArrayList<BookMark>) {
        mBookMarkList = list
        notifyDataSetChanged()
    }

    fun attachListener(listener: BookMarkListener) {
        mListener = listener
    }

    fun notifyItemChanged(mark: BookMark) {
        if (mBookMarkList.contains(mark))
            notifyItemChanged(mBookMarkList.indexOf(mark))
    }

}