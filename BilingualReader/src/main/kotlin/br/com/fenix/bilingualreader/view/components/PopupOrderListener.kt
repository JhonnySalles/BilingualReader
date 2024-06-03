package br.com.fenix.bilingualreader.view.components

import androidx.lifecycle.LiveData
import br.com.fenix.bilingualreader.model.enums.Order

interface PopupOrderListener {
    fun popupOrderOnChange()
    fun popupSorted(order : Order)
    fun popupSorted(order : Order, isDesc : Boolean)

    fun popupGetOrder() : Pair<Order, Boolean>?
    fun popupGetObserver() : LiveData<Pair<Order, Boolean>>
}