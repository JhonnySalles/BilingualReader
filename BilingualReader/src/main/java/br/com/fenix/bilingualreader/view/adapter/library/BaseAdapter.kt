package br.com.fenix.bilingualreader.view.adapter.library

import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.BaseCardListener

interface BaseAdapter<T, L : BaseCardListener> {
    abstract var isAnimation: Boolean
    fun removeList(item: T);
    fun updateList(order: Order, list: MutableList<T>)
    fun attachListener(listener: L)
}