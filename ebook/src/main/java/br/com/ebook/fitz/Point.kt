package br.com.ebook.fitz

class Point {
    var x: Float
    var y: Float

    constructor(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    constructor(p: Point) {
        x = p.x
        y = p.y
    }

    override fun toString(): String {
        return "[$x $y]"
    }

    fun transform(tm: Matrix): Point {
        val old_x = x
        x = old_x * tm.a + y * tm.c + tm.e
        y = old_x * tm.b + y * tm.d + tm.f
        return this
    }
}