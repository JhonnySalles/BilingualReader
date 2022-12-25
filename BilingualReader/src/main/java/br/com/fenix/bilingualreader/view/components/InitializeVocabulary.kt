package br.com.fenix.bilingualreader.view.components

interface InitializeVocabulary<O> {
    fun setVocabulary(vocabulary: String)
    fun setObject(obj: O)
}