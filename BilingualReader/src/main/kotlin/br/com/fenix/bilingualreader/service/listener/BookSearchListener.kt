package br.com.fenix.bilingualreader.service.listener

import android.view.View
import br.com.fenix.bilingualreader.model.entity.BookSearch

interface BookSearchListener {
    fun onClick(search: BookSearch)
    fun onClickLong(search: BookSearch, view: View, position: Int)
}