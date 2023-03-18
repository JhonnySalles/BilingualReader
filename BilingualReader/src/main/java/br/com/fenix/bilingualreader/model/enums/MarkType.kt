package br.com.fenix.bilingualreader.model.enums

import br.com.fenix.bilingualreader.R

enum class MarkType(private val description: Int,) {
    BookMark(R.string.book_annotation_type_annotation),
    Annotation(R.string.book_annotation_type_book_mark);

    open fun getDescription() : Int = this.description
}