package br.com.fenix.bilingualreader.view.adapter.annotation

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.service.listener.BookAnnotationListener


class AnnotationTitleViewHolder(itemView: View, private val listener: BookAnnotationListener) : RecyclerView.ViewHolder(itemView) {

    fun bind(annotation: BookAnnotation, isFirst: Boolean) {
        itemView.findViewById<View>(R.id.book_annotation_divider_separator).visibility = if (isFirst) View.GONE else View.VISIBLE
        itemView.findViewById<TextView>(R.id.book_annotation_divider_title).text = itemView.context.getString(
                R.string.book_annotation_list_divide_title,
                annotation.chapterNumber,
                annotation.chapter
            )
        itemView.findViewById<TextView>(R.id.book_annotation_divider_count).text = itemView.context.getString(
                R.string.book_annotation_list_divide_count,
                annotation.count
            )
    }

}