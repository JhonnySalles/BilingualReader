package br.com.fenix.bilingualreader.view.adapter.annotation

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.interfaces.Annotation
import br.com.fenix.bilingualreader.service.listener.BookAnnotationListener


class AnnotationBookViewHolder(itemView: View, private val listener: BookAnnotationListener) : RecyclerView.ViewHolder(itemView) {

    fun bind(annotation: BookAnnotation, isFirst: Boolean) {
        itemView.findViewById<TextView>(R.id.book_title_divider_title).text = annotation.text
        itemView.findViewById<TextView>(R.id.book_title_divider_sub_title).text = annotation.annotation
    }

}