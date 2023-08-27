package br.com.ebook.fitz

import br.com.ebook.fitz.Context.init

class DisplayList {
    private var pointer: Long
    protected external fun finalize()
    fun destroy() {
        finalize()
        pointer = 0
    }

    private external fun newNative(): Long

    constructor() {
        pointer = newNative()
    }

    private constructor(p: Long) {
        pointer = p
    }

    external fun toPixmap(ctm: Matrix?, colorspace: ColorSpace?, alpha: Boolean): Pixmap?
    @JvmOverloads
    external fun toStructuredText(options: String? = null): StructuredText?
    external fun search(needle: String?): Array<Rect?>?
    external fun run(dev: Device?, ctm: Matrix?, scissor: Rect?, cookie: Cookie?)
    fun run(dev: Device?, ctm: Matrix?, cookie: Cookie?) {
        run(dev, ctm, null, cookie)
    }

    companion object {
        init {
            init()
        }
    }
}