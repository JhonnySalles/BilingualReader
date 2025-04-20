package br.com.fenix.bilingualreader.view.adapter.annotation

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.service.listener.AnnotationsListener
import br.com.fenix.bilingualreader.util.helpers.Util


class AnnotationMangaViewHolder(itemView: View, private val listener: AnnotationsListener) : RecyclerView.ViewHolder(itemView) {

    fun bind(mark: MangaAnnotation, position: Int) {
        val root = itemView.findViewById<LinearLayout>(R.id.manga_annotation_root)

        val title = itemView.findViewById<TextView>(R.id.manga_annotation_title)
        val text = itemView.findViewById<TextView>(R.id.manga_annotation_page)

        root.setOnClickListener { listener.onClick(mark) }

        title.text = Util.getNameFromPath(mark.folder)
        text.text = itemView.context.getString(R.string.manga_annotation_title_mark, mark.page, mark.pages)
    }

}