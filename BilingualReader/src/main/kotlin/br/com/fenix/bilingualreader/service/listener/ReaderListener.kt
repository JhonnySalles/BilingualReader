package br.com.fenix.bilingualreader.service.listener

interface ReaderListener : BaseCardListener {
    fun setCurrentPage(page : Int)
}