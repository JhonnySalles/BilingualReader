package br.com.fenix.bilingualreader.view.adapter.book

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.service.listener.BookAnnotationListener

class BookAnnotationHeaderViewHolder(itemView: View, private val listener: BookAnnotationListener) :
    RecyclerView.ViewHolder(itemView) {

    fun bind(mark: BookAnnotation, isFirst: Boolean) {
        itemView.findViewById<View>(R.id.book_mark_divider_separator).visibility =
            if (isFirst) View.GONE else View.VISIBLE

        itemView.findViewById<TextView>(R.id.book_mark_divider_title).text =
            itemView.context.getString(
                R.string.book_annotation_list_divide_title,
                mark.chapterNumber,
                mark.chapter
            )

        itemView.findViewById<TextView>(R.id.book_mark_divider_count).text =
            itemView.context.getString(
                R.string.book_annotation_list_divide_count,
                mark.count
            )
    }

}