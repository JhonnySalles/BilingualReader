package br.com.fenix.bilingualreader.view.adapter.chapters

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Chapters
import br.com.fenix.bilingualreader.service.listener.ChapterCardListener

class ChaptersHeaderViewHolder(itemView: View, private val listener: ChapterCardListener) :
    RecyclerView.ViewHolder(itemView) {

    fun bind(chapters: Chapters) {
        itemView.findViewById<TextView>(R.id.chapters_title).text = chapters.title
    }

}