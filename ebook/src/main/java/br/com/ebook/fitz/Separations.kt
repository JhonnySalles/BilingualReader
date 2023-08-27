package br.com.ebook.fitz

import br.com.ebook.fitz.Context.init

class Separations protected constructor(private var pointer: Long) {
    protected external fun finalize()
    fun destroy() {
        finalize()
        pointer = 0
    }

    val numberOfSeparations: Int
        external get

    external fun getSeparation(separation: Int): Separation?
    external fun areSeparationsControllable(): Boolean
    external fun getSeparationBehavior(separation: Int): Int
    external fun setSeparationBehavior(separation: Int, behavior: Int)

    companion object {
        init {
            init()
        }

        const val SEPARATION_COMPOSITE = 0
        const val SEPARATION_SPOT = 1
        const val SEPARATION_DISABLED = 2
    }
}