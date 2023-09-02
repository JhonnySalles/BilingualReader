package br.com.fenix.bilingualreader.view.adapter.library

import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.BaseCardListenner
import br.com.fenix.bilingualreader.service.listener.MangaCardListener

interface BaseAdapter<T, L : BaseCardListenner> {
    abstract var isAnimation: Boolean
    fun removeList(item: T);
    fun updateList(order: Order, list: MutableList<T>)
    fun attachListener(listener: L)
}