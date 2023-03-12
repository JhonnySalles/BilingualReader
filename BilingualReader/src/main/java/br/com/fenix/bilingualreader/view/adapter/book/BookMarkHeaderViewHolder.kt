package br.com.fenix.bilingualreader.view.adapter.book

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookMark
import br.com.fenix.bilingualreader.service.listener.BookMarkListener

class BookMarkHeaderViewHolder(itemView: View, private val listener: BookMarkListener) :
    RecyclerView.ViewHolder(itemView) {

    fun bind(mark: BookMark, isFirst: Boolean) {
        itemView.findViewById<View>(R.id.book_mark_divider_separator).visibility =
            if (isFirst) View.GONE else View.VISIBLE

        itemView.findViewById<TextView>(R.id.book_mark_divider_title).text =
            itemView.context.getString(
                R.string.book_mark_list_divide_title,
                mark.chapterNumber,
                mark.chapter
            )

        itemView.findViewById<TextView>(R.id.book_mark_divider_count).text =
            itemView.context.getString(
                R.string.book_mark_list_divide_count,
                mark.count
            )
    }

}