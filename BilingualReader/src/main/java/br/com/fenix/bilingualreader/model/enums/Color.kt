package br.com.fenix.bilingualreader.model.enums

enum class Color(private val index: Int, private val hexHtml: String, private val color: Int) {
    None(-1, "", 0),
    Blue(0, "", 0),
    Green(1, "", 0),
    Red(2, "", 0),
    Yellow(3, "", 0);

    open fun getIndex() : Int = this.index
    open fun getHtmlColor() : String = this.hexHtml
    open fun getColor() : Int = this.color
}