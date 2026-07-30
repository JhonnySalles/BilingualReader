package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.interfaces.History

data class HistorySeriesGroup(
    val series: String,
    val items: MutableList<History>
)
