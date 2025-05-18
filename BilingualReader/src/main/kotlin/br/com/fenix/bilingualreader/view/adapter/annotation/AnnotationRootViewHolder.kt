package br.com.fenix.bilingualreader.view.adapter.annotation

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.interfaces.Annotation
import br.com.fenix.bilingualreader.service.listener.AnnotationsListener


class AnnotationRootViewHolder(itemView: View, private val listener: AnnotationsListener) : RecyclerView.ViewHolder(itemView) {

    fun bind(annotation: Annotation, isFirst: Boolean) {
        itemView.findViewById<TextView>(R.id.annotation_title_divider_title).text = annotation.chapter
        itemView.findViewById<TextView>(R.id.annotation_title_divider_sub_title).text = annotation.annotation
    }

}