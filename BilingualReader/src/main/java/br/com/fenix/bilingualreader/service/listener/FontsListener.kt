package br.com.fenix.bilingualreader.service.listener

import br.com.fenix.bilingualreader.model.enums.FontType

interface FontsListener {
    fun onClick(font: Pair<FontType, Boolean>)
}