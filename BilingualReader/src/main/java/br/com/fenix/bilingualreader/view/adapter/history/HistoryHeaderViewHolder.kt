package br.com.fenix.bilingualreader.view.adapter.history

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.service.listener.MangaCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts

class HistoryHeaderViewHolder(itemView: View, private val listener: MangaCardListener) :
    RecyclerView.ViewHolder(itemView) {

    fun bind(manga: Manga) {
        itemView.findViewById<TextView>(R.id.history_divider_title).text = GeneralConsts.formatCountDays(itemView.context, manga.lastAccess)
    }

}