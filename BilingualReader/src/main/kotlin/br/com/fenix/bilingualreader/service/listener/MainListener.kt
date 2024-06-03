package br.com.fenix.bilingualreader.service.listener

interface MainListener {
    fun showUpButton()
    fun hideUpButton()

    fun changeLibraryTitle(library: String)
    fun clearLibraryTitle()
}