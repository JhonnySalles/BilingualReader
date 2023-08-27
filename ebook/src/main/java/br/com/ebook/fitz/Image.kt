package br.com.ebook.fitz

import br.com.ebook.fitz.Context.init

class Image {
    protected var pointer: Long
    protected external fun finalize()
    fun destroy() {
        finalize()
        pointer = 0
    }

    private external fun newNativeFromPixmap(pixmap: Pixmap): Long
    private external fun newNativeFromFile(filename: String): Long

    protected constructor(p: Long) {
        pointer = p
    }

    constructor(pixmap: Pixmap) {
        pointer = newNativeFromPixmap(pixmap)
    }

    constructor(filename: String) {
        pointer = newNativeFromFile(filename)
    }

    val width: Int
        external get
    val height: Int
        external get
    val xResolution: Int
        external get
    val yResolution: Int
        external get
    val colorSpace: ColorSpace?
        external get
    val numberOfComponents: Int
        external get
    val bitsPerComponent: Int
        external get
    val imageMask: Boolean
        external get
    val interpolate: Boolean
        external get
    val mask: Image?
        external get

    external fun toPixmap(): Pixmap?

    companion object {
        init {
            init()
        }
    }
}