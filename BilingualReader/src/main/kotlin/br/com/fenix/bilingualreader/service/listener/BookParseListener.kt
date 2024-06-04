package br.com.fenix.bilingualreader.service.listener

interface BookParseListener {
    fun onLoading(isFinished: Boolean, isLoaded : Boolean = false)
    fun onSearching(isSearching: Boolean)
    fun onConverting(isConverting: Boolean)
}