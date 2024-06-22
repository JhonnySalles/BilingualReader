package br.com.fenix.bilingualreader.model.interfaces

interface Annotation {
    val id_book: Long
    val chapterNumber: Float
    var parent: Annotation?
    var isRoot: Boolean
    var isTitle: Boolean
    var count: Int
}