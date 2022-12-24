package br.com.fenix.bilingualreader.service.listener

import br.com.fenix.bilingualreader.model.entity.Library

interface LibrariesCardListener {
    fun onClickLong(library: Library)
    fun changeEnable(library: Library)
}