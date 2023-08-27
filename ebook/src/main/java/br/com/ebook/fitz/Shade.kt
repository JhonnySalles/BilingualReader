package br.com.ebook.fitz

import br.com.ebook.fitz.Context.init

class Shade private constructor(private var pointer: Long) {
    protected external fun finalize()
    fun destroy() {
        finalize()
        pointer = 0
    }

    companion object {
        init {
            init()
        }
    }
}