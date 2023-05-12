package br.com.fenix.bilingualreader.view.adapter.reader

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Chapters
import br.com.fenix.bilingualreader.service.listener.ChapterCardListener
import com.google.android.material.card.MaterialCardView


class MangaChaptersViewHolder(itemView: View, private val listener: ChapterCardListener) :
    RecyclerView.ViewHolder(itemView) {

    companion object {
        var mPageSelectStroke: Int = 0
    }

    init {
        mPageSelectStroke = itemView.resources.getDimension(R.dimen.manga_chapter_selected_stroke).toInt()
    }

    fun bind(page: Chapters) {
        val card = itemView.findViewById<MaterialCardView>(R.id.manga_chapter_card)
        val image = itemView.findViewById<ImageView>(R.id.manga_chapter_image)
        val number = itemView.findViewById<TextView>(R.id.manga_chapter_number)

        card.strokeWidth = if (page.isSelected) mPageSelectStroke else 0

        number.text = page.page.toString()
        card.setOnClickListener { listener.onClick(page) }
        image.setImageBitmap(page.image)
    }

}