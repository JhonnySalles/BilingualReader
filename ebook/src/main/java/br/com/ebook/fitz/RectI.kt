package br.com.ebook.fitz

class RectI {
    var x0: Int
    var y0: Int
    var x1: Int
    var y1: Int

    constructor() {
        y1 = 0
        x1 = y1
        y0 = x1
        x0 = y0
    }

    constructor(x0: Int, y0: Int, x1: Int, y1: Int) {
        this.x0 = x0
        this.y0 = y0
        this.x1 = x1
        this.y1 = y1
    }

    constructor(r: RectI) : this(r.x0, r.y0, r.x1, r.y1) {}
    constructor(r: Rect) {
        x0 = Math.floor(r.x0.toDouble()).toInt()
        y0 = Math.ceil(r.y0.toDouble()).toInt()
        x1 = Math.floor(r.x1.toDouble()).toInt()
        y1 = Math.ceil(r.y1.toDouble()).toInt()
    }

    override fun toString(): String {
        return "[$x0 $y0 $x1 $y1]"
    }

    fun transform(tm: Matrix): RectI {
        var ax0 = x0 * tm.a
        var ax1 = x1 * tm.a
        if (ax0 > ax1) {
            val t = ax0
            ax0 = ax1
            ax1 = t
        }
        var cy0 = y0 * tm.c
        var cy1 = y1 * tm.c
        if (cy0 > cy1) {
            val t = cy0
            cy0 = cy1
            cy1 = t
        }
        ax0 += cy0 + tm.e
        ax1 += cy1 + tm.e
        var bx0 = x0 * tm.b
        var bx1 = x1 * tm.b
        if (bx0 > bx1) {
            val t = bx0
            bx0 = bx1
            bx1 = t
        }
        var dy0 = y0 * tm.d
        var dy1 = y1 * tm.d
        if (dy0 > dy1) {
            val t = dy0
            dy0 = dy1
            dy1 = t
        }
        bx0 += dy0 + tm.f
        bx1 += dy1 + tm.f
        x0 = Math.floor(ax0.toDouble()).toInt()
        x1 = Math.ceil(ax1.toDouble()).toInt()
        y0 = Math.floor(bx0.toDouble()).toInt()
        y1 = Math.ceil(bx1.toDouble()).toInt()
        return this
    }

    fun contains(x: Int, y: Int): Boolean {
        return if (isEmpty) false else x >= x0 && x < x1 && y >= y0 && y < y1
    }

    operator fun contains(r: Rect): Boolean {
        return if (isEmpty || r.isEmpty) false else r.x0 >= x0 && r.x1 <= x1 && r.y0 >= y0 && r.y1 <= y1
    }

    val isEmpty: Boolean
        get() = x0 == x1 || y0 == y1

    fun union(r: RectI) {
        if (isEmpty) {
            x0 = r.x0
            y0 = r.y0
            x1 = r.x1
            y1 = r.y1
        } else {
            if (r.x0 < x0) x0 = r.x0
            if (r.y0 < y0) y0 = r.y0
            if (r.x1 > x1) x1 = r.x1
            if (r.y1 > y1) y1 = r.y1
        }
    }
}