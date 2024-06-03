package br.com.fenix.bilingualreader.service.listener

import android.view.View
import br.com.fenix.bilingualreader.model.entity.Book

interface BookCardListener: BaseCardListener {
    fun onClick(book: Book)
    fun onClickFavorite(book: Book)
    fun onClickConfig(book: Book, root: View, item: View, position: Int)

    fun onClickLong(book: Book, view: View, position: Int)
    fun onClickLongConfig(book: Book, root: View, item: View, position: Int)
}