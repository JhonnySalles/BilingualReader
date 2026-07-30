package br.com.fenix.bilingualreader.view.adapter.history

import br.com.fenix.bilingualreader.model.interfaces.History
import br.com.fenix.bilingualreader.service.listener.HistoryCardListener

interface HistoryBaseAdapter {
    var isAnimation: Boolean
    fun updateList(list: ArrayList<Any>)
    fun getItem(position: Int): History?
    fun remove(history: History)
    fun notifyItemChanged(history: History)
    fun attachListener(listener: HistoryCardListener)
}
