package br.com.fenix.bilingualreader.service.listener

import android.view.View
import br.com.fenix.bilingualreader.model.entity.Book

interface BookCardListener {
    fun onClick(book: Book)
    fun onClickFavorite(book: Book, view: View, position: Int)
    fun onClickConfig(book: Book, view: View, position: Int)

    fun onClickLong(book: Book, view: View, position: Int)
    fun onClickLongConfig(book: Book, view: View, position: Int)
}