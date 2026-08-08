package br.com.fenix.bilingualreader.model.entity

data class BookGroup(
    val title: String,
    val items: MutableList<Book>
)
