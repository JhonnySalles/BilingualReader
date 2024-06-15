package br.com.fenix.bilingualreader.view.adapter.history

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.listener.HistoryCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts

class HistoryHeaderViewHolder(itemView: View, private val listener: HistoryCardListener) : RecyclerView.ViewHolder(itemView) {

    fun bind(history: History) {
        itemView.findViewById<TextView>(R.id.history_divider_title).text = GeneralConsts.formatCountDays(itemView.context, history.lastAccess)
    }

}