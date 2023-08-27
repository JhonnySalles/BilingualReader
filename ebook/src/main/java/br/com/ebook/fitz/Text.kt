package br.com.ebook.fitz

import br.com.ebook.fitz.Context.init

class Text : TextWalker {
    private var pointer: Long
    protected external fun finalize()
    fun destroy() {
        finalize()
        pointer = 0
    }

    private external fun newNative(): Long
    private external fun cloneNative(old: Text): Long

    private constructor(p: Long) {
        pointer = p
    }

    constructor(old: Text) {
        pointer = cloneNative(old)
    }

    constructor() {
        pointer = newNative()
    }

    external override fun showGlyph(font: Font?, trm: Matrix?, glyph: Int, unicode: Int, wmode: Boolean)
    @JvmOverloads
    external fun showString(font: Font?, trm: Matrix?, str: String?, wmode: Boolean = false)
    external fun getBounds(stroke: StrokeState?, ctm: Matrix?): Rect?
    fun showGlyph(font: Font, trm: Matrix, glyph: Int, unicode: Int) {
        showGlyph(font, trm, glyph, unicode, false)
    }

    external fun walk(walker: TextWalker?)

    companion object {
        init {
            init()
        }
    }
}