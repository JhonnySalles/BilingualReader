package br.com.fenix.bilingualreader.view.adapter.annotation

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.service.listener.AnnotationsListener


class AnnotationViewHolder(itemView: View, private val listener: AnnotationsListener) : RecyclerView.ViewHolder(itemView) {

    fun bind(mark: BookAnnotation, position: Int) { }

}