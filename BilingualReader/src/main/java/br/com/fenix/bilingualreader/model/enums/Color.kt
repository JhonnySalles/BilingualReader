package br.com.fenix.bilingualreader.model.enums

import br.com.fenix.bilingualreader.R

enum class Color(private val index: Int, private val hexHtml: String, private val color: Int, private val description: Int) {
    None(-1, "", 0, R.string.book_annotation_color_none),
    Blue(0, "", 0, R.string.book_annotation_color_blue),
    Green(1, "", 0, R.string.book_annotation_color_green),
    Red(2, "", 0, R.string.book_annotation_color_red),
    Yellow(3, "", 0, R.string.book_annotation_color_yellow);

    open fun getIndex() : Int = this.index
    open fun getHtmlColor() : String = this.hexHtml
    open fun getColor() : Int = this.color
    open fun getDescription() : Int = this.description

    companion object {
        fun getColors() = Color.values()
    }
}