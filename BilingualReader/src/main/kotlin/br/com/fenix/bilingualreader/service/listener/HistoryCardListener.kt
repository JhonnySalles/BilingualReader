package br.com.fenix.bilingualreader.service.listener

import android.view.View
import br.com.fenix.bilingualreader.model.interfaces.History

interface HistoryCardListener : BaseCardListener {
    fun onClick(history: History)
    fun onClickFavorite(history: History) {}
    fun onClickConfig(history: History, root: View, item: View, position: Int) {}
    fun onClickLong(history: History, view : View, position: Int)
}