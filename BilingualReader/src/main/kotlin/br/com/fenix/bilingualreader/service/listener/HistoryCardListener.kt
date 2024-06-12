package br.com.fenix.bilingualreader.service.listener

import android.view.View
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.interfaces.History

interface HistoryCardListener : BaseCardListener {
    fun onClick(history: History)
    fun onClickLong(history: History, view : View, position: Int)
}