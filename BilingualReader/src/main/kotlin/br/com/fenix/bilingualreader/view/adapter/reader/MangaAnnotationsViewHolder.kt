package br.com.fenix.bilingualreader.view.adapter.reader

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Chapters
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.service.listener.ChapterCardListener
import br.com.fenix.bilingualreader.service.listener.MangaAnnotationListener
import com.google.android.material.card.MaterialCardView


class MangaAnnotationsViewHolder(itemView: View, private val listener: MangaAnnotationListener) : RecyclerView.ViewHolder(itemView) {

    companion object {
        var mPageSelectStroke: Int = 0
    }

    init {
        mPageSelectStroke = itemView.resources.getDimension(R.dimen.manga_annotation_selected_stroke).toInt()
    }

    fun bind(page: MangaAnnotation) {
        val card = itemView.findViewById<MaterialCardView>(R.id.manga_annotation_card)
        val image = itemView.findViewById<ImageView>(R.id.manga_annotation_image)
        val number = itemView.findViewById<TextView>(R.id.manga_annotation_number)

        card.strokeWidth = if (page.isSelected) mPageSelectStroke else 0

        number.text = page.page.toString()
        card.setOnClickListener { listener.onClick(page) }
        image.setImageBitmap(page.image)
    }

}