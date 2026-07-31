package br.com.fenix.bilingualreader.model.entity

data class MangaGroup(
    val title: String,
    val items: MutableList<Manga>
)
