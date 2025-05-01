package br.com.fenix.bilingualreader.service.listener


interface SelectionChangeListener {
    fun isShowingPopup() : Boolean
    fun onTextSelected()
    fun onTextUnselected()
}