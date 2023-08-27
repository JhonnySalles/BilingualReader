package br.com.ebook.fitz

class Cookie {
    private var pointer: Long
    protected external fun finalize()
    fun destroy() {
        finalize()
        pointer = 0
    }

    private external fun newNative(): Long

    init {
        pointer = newNative()
    }

    external fun abort()

    companion object {
        init {
            Context.init()
        }
    }
}