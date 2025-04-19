package br.com.fenix.bilingualreader.view.adapter.reader

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Chapters
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation

class MangaAnnotationHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun bind(annotation: MangaAnnotation) {
        itemView.findViewById<TextView>(R.id.manga_annotation_separator_title).text = annotation.chapter
    }

}