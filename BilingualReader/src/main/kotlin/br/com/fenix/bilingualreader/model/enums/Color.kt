package br.com.fenix.bilingualreader.model.enums

import br.com.fenix.bilingualreader.R


enum class Color(private val index: Int, private val hexHtml: String, private val color: Int, private val description: Int) {
    None(-1, "", -1, R.string.annotation_colors_none),
    Blue(0, "#668cff", R.color.text_select_blue, R.string.annotation_colors_blue),
    Green(1, "#00e600", R.color.text_select_green, R.string.annotation_colors_green),
    Red(2, "#ff4d4d", R.color.text_select_red, R.string.annotation_colors_red),
    Yellow(3, "#e6b800", R.color.text_select_yellow, R.string.annotation_colors_yellow);

    open fun getIndex() : Int = this.index
    open fun getHtmlColor() : String = this.hexHtml
    open fun getColor() : Int = this.color
    open fun getDescription() : Int = this.description

    companion object {
        fun getColors() = Color.values()
    }
}