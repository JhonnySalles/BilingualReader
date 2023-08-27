package br.com.ebook.fitz

interface TextWalker {
    fun showGlyph(font: Font?, trm: Matrix?, glyph: Int, unicode: Int, wmode: Boolean)
}