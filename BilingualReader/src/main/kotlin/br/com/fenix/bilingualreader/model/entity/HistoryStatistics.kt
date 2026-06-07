package br.com.fenix.bilingualreader.model.entity

import br.com.fenix.bilingualreader.model.interfaces.History
import java.time.LocalDateTime

data class HistoryStatistics(
    val base: History,
    val timeRead: Long, // in seconds
    val pagesRead: Int,
    override var sort: LocalDateTime? = base.sort,
    override var lastAccess: LocalDateTime? = base.lastAccess
) : History by base
