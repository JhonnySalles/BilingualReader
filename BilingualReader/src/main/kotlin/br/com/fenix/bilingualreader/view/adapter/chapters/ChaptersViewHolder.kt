package br.com.fenix.bilingualreader.view.adapter.chapters

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Chapters
import br.com.fenix.bilingualreader.service.listener.ChapterCardListener
import com.google.android.material.card.MaterialCardView

class ChaptersViewHolder(itemView: View, private val listener: ChapterCardListener) : RecyclerView.ViewHolder(itemView) {

    companion object {
        var mPageSelectStroke: Int = 0
    }

    init {
        mPageSelectStroke = itemView.resources.getDimension(R.dimen.manga_chapter_selected_stroke).toInt()
    }

    fun bind(chapter: Chapters) {
        val root = itemView.findViewById<LinearLayout>(R.id.chapter_root)
        val card = itemView.findViewById<MaterialCardView>(R.id.chapter_grid_card)
        val image = itemView.findViewById<ImageView>(R.id.chapters_page_image)
        val page = itemView.findViewById<TextView>(R.id.chapters_page_number)

        card.strokeWidth = if (chapter.isSelected) mPageSelectStroke else 0

        root.setOnClickListener { listener.onClick(chapter) }
        root.setOnLongClickListener {
            listener.onLongClick(chapter)
            true
        }
        image.setImageBitmap(chapter.image)
        page.text = "${chapter.page}"
    }

}