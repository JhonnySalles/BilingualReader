package br.com.ebook.fitz

class Annotation protected constructor(private var pointer: Long) {
    protected external fun finalize()
    fun destroy() {
        finalize()
        pointer = 0
    }

    external fun run(dev: Device?, ctm: Matrix?, cookie: Cookie?)
    external fun toPixmap(ctm: Matrix?, colorspace: ColorSpace?, alpha: Boolean): Pixmap?
    val bounds: Rect?
        external get

    external fun toDisplayList(): DisplayList?
    private external fun advance(): Long

    companion object {
        init {
            Context.init()
        }
    }
}