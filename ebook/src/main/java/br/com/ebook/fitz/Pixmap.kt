package br.com.ebook.fitz

import br.com.ebook.fitz.Context.init

class Pixmap {
    private var pointer: Long
    protected external fun finalize()
    fun destroy() {
        finalize()
        pointer = 0
    }

    private external fun newNative(cs: ColorSpace, x: Int, y: Int, w: Int, h: Int, alpha: Boolean): Long

    private constructor(p: Long) {
        pointer = p
    }

    @JvmOverloads
    constructor(cs: ColorSpace, x: Int, y: Int, w: Int, h: Int, alpha: Boolean = false) {
        pointer = newNative(cs, x, y, w, h, alpha)
    }

    constructor(cs: ColorSpace, w: Int, h: Int, alpha: Boolean) : this(cs, 0, 0, w, h, alpha) {}
    constructor(cs: ColorSpace, w: Int, h: Int) : this(cs, 0, 0, w, h, false) {}

    @JvmOverloads
    constructor(cs: ColorSpace, rect: Rect, alpha: Boolean = false) : this(
        cs,
        rect.x0.toInt(),
        rect.y0.toInt(),
        (rect.x1 - rect.x0).toInt(),
        (rect.y1 - rect.y0).toInt(),
        alpha
    ) {
    }

    external fun clear()
    private external fun clearWithValue(value: Int)
    fun clear(value: Int) {
        clearWithValue(value)
    }

    external fun saveAsPNG(filename: String?)
    val x: Int
        external get
    val y: Int
        external get
    val width: Int
        external get
    val height: Int
        external get
    val stride: Int
        external get
    val numberOfComponents: Int
        external get
    val alpha: Boolean
        external get
    val colorSpace: ColorSpace
        external get
    val samples: ByteArray?
        external get

    external fun getSample(x: Int, y: Int, n: Int): Byte

    /* only valid for RGBA or BGRA pixmaps */
    val pixels: IntArray?
        external get
    val xResolution: Int
        external get
    val yResolution: Int
        external get
    val bounds: Rect
        get() {
            val x = x
            val y = y
            return Rect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat())
        }

    override fun toString(): String {
        return "Pixmap(w=" + width +
                " h=" + height +
                " x=" + x +
                " y=" + y +
                " n=" + numberOfComponents +
                " alpha=" + alpha +
                " cs=" + colorSpace +
                ")"
    }

    companion object {
        init {
            init()
        }
    }
}