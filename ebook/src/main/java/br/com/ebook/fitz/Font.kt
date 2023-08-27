package br.com.ebook.fitz

import br.com.ebook.fitz.Context.init

class Font {
    private var pointer: Long
    protected external fun finalize()
    fun destroy() {
        finalize()
        pointer = 0
    }

    private external fun newNative(name: String, index: Int): Long

    private constructor(p: Long) {
        pointer = p
    }

    constructor(name: String, index: Int) {
        pointer = newNative(name, index)
    }

    constructor(name: String) {
        pointer = newNative(name, 0)
    }

    val name: String
        external get

    external fun encodeCharacter(unicode: Int): Int
    @JvmOverloads
    external fun advanceGlyph(glyph: Int, wmode: Boolean = false): Float
    override fun toString(): String {
        return "Font(" + name + ")"
    }

    companion object {
        init {
            init()
        }
    }
}