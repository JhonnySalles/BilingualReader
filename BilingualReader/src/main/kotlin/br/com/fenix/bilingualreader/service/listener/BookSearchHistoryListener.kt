package br.com.fenix.bilingualreader.service.listener

import android.view.View
import br.com.fenix.bilingualreader.model.entity.BookSearch

interface BookSearchHistoryListener {
    fun onClick(search: BookSearch)
    fun onDelete(search: BookSearch, view: View, position: Int)
}