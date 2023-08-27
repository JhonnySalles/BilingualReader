package br.com.ebook.fitz

import br.com.ebook.fitz.Context.init

class Path : PathWalker {
    private var pointer: Long
    protected external fun finalize()
    fun destroy() {
        finalize()
        pointer = 0
    }

    private external fun newNative(): Long
    private external fun cloneNative(): Long

    constructor() {
        pointer = newNative()
    }

    private constructor(p: Long) {
        pointer = p
    }

    constructor(old: Path) {
        pointer = old.cloneNative()
    }

    external fun currentPoint(): Point?
    external override fun moveTo(x: Float, y: Float)
    external override fun lineTo(x: Float, y: Float)
    external override fun curveTo(cx1: Float, cy1: Float, cx2: Float, cy2: Float, ex: Float, ey: Float)
    external fun curveToV(cx: Float, cy: Float, ex: Float, ey: Float)
    external fun curveToY(cx: Float, cy: Float, ex: Float, ey: Float)
    external fun rect(x1: Int, y1: Int, x2: Int, y2: Int)
    external override fun closePath()
    fun moveTo(xy: Point) {
        moveTo(xy.x, xy.y)
    }

    fun lineTo(xy: Point) {
        lineTo(xy.x, xy.y)
    }

    fun curveTo(c1: Point, c2: Point, e: Point) {
        curveTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
    }

    fun curveToV(c: Point, e: Point) {
        curveToV(c.x, c.y, e.x, e.y)
    }

    fun curveToY(c: Point, e: Point) {
        curveToY(c.x, c.y, e.x, e.y)
    }

    external fun transform(mat: Matrix?)
    external fun getBounds(stroke: StrokeState?, ctm: Matrix?): Rect?
    external fun walk(walker: PathWalker?)

    companion object {
        init {
            init()
        }
    }
}