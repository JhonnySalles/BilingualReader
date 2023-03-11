package br.com.fenix.bilingualreader.service.listener

import android.view.View
import br.com.fenix.bilingualreader.model.entity.BookMark

interface BookMarkListener {
    fun onClick(mark: BookMark)
    fun onClickLong(mark: BookMark, view: View, position: Int)
    fun onClickFavorite(mark: BookMark)
    fun onClickOptions(mark: BookMark, view: View, position: Int)
    fun onClickNote(mark: BookMark)
}