package br.com.ebook.fitz

import br.com.ebook.fitz.Context.init

class StrokeState {
    private var pointer: Long
    protected external fun finalize()
    fun destroy() {
        finalize()
        pointer = 0
    }

    private external fun newNative(
        startCap: Int, dashCap: Int, endCap: Int, lineJoin: Int, lineWidth: Float, miterLimit: Float,
        dashPhase: Float, dash: FloatArray?
    ): Long

    // Private constructor for the C to use. Any objects created by the
    // C are done for purposes of calling back to a java device, and
    // should therefore be considered const. This is fine as we don't
    // currently provide mechanisms for changing individual elements
    // of the StrokeState.
    private constructor(p: Long) {
        pointer = p
    }

    constructor(startCap: Int, endCap: Int, lineJoin: Int, lineWidth: Float, miterLimit: Float) {
        pointer = newNative(startCap, 0, endCap, lineJoin, lineWidth, miterLimit, 0f, null)
    }

    constructor(
        startCap: Int, dashCap: Int, endCap: Int, lineJoin: Int, lineWidth: Float, miterLimit: Float,
        dashPhase: Float, dash: FloatArray?
    ) {
        pointer = newNative(startCap, dashCap, endCap, lineJoin, lineWidth, miterLimit, dashPhase, dash)
    }

    val startCap: Int
        external get
    val dashCap: Int
        external get
    val endCap: Int
        external get
    val lineJoin: Int
        external get
    val lineWidth: Float
        external get
    val miterLimit: Float
        external get
    val dashPhase: Float
        external get
    val dashes: FloatArray?
        external get

    companion object {
        init {
            init()
        }

        const val LINECAP_BUTT = 0
        const val LINECAP_ROUND = 1
        const val LINECAP_SQUARE = 2
        const val LINECAP_TRIANGLE = 3
        const val LINEJOIN_MITER = 0
        const val LINEJOIN_ROUND = 1
        const val LINEJOIN_BEVEL = 2
        const val LINEJOIN_MITER_XPS = 3
    }
}