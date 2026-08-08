package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.interfaces.History

data class HistoryGroup(
    val title: String,
    val items: MutableList<History>
)
