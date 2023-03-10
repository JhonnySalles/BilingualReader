package br.com.fenix.bilingualreader.service.listener

import android.view.View
import br.com.fenix.bilingualreader.model.entity.Book

interface BookCardListener {
    fun onClick(book: Book)
    fun onClickLong(book: Book, view: View, position: Int)
}