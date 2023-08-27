package br.com.ebook.fitz

import br.com.ebook.fitz.Context.init

class StructuredText {
    private var pointer: Long
    protected external fun finalize()
    fun destroy() {
        finalize()
        pointer = 0
    }

    private constructor(p: Long) {
        pointer = p
    }

    private constructor() {
        pointer = 0
    }

    external fun search(needle: String?): Array<Rect?>?
    external fun highlight(a: Point?, b: Point?): Array<Rect?>?
    external fun copy(a: Point?, b: Point?): String?

    inner class TextBlock {
        @JvmField
		var lines: Array<TextLine>? = null
        var bbox: Rect? = null
    }

    inner class TextLine {
        @JvmField
		var chars: Array<TextChar>? = null
        var bbox: Rect? = null
    }

    inner class TextChar {
        @JvmField
		var c = 0
        @JvmField
		var bbox: Rect? = null
        val isWhitespace: Boolean
            get() = Character.isWhitespace(c)
    }

    companion object {
        init {
            init()
        }

        @JvmStatic
        val blocks: Array<TextBlock?>?
            external get

        external fun initNative(): Int
        @JvmStatic
        external fun getBlocks(doc: Long, page: Long): Array<TextBlock?>?
    }
}