package br.com.fenix.bilingualreader.service.listener

import br.com.fenix.bilingualreader.model.enums.Themes

interface ThemesListener {
    fun onClick(theme: Pair<Themes, Boolean>)
}