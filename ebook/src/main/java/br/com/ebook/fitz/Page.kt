package br.com.ebook.fitz

import br.com.ebook.fitz.Context.init

class Page(private var pointer: Long) {
    protected external fun finalize()
    fun destroy() {
        finalize()
        pointer = 0
    }

    val bounds: Rect?
        external get

    external fun toPixmap(ctm: Matrix?, cs: ColorSpace?, alpha: Boolean): Pixmap?
    @JvmOverloads
    external fun run(dev: Device?, ctm: Matrix?, cookie: Cookie? = null)
    external fun runPageContents(dev: Device?, ctm: Matrix?, cookie: Cookie?)
    val annotations: Array<Annotation?>?
        external get
    val links: Array<Link?>?
        external get

    // FIXME: Later. Much later.
    //fz_transition *fz_page_presentation(fz_document *doc, fz_page *page, float *duration);
    external fun toDisplayList(no_annotations: Boolean): DisplayList?
    @JvmOverloads
    external fun toStructuredText(options: String? = null): StructuredText?
    external fun search(needle: String?): Array<Rect?>?
    external fun textAsHtml(): ByteArray?
    val separations: Separations?
        external get

    companion object {
        init {
            init()
        }
    }
}