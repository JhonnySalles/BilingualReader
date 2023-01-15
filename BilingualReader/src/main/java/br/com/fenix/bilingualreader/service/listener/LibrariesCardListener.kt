package br.com.fenix.bilingualreader.service.listener

import android.view.View
import br.com.fenix.bilingualreader.model.entity.Library

interface LibrariesCardListener {
    fun onClick(library: Library)
    fun onClickLong(library: Library, view : View, position: Int)
    fun changeEnable(library: Library)
}