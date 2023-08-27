package br.com.ebook.fitz

class Matrix {
    @JvmField
	var a: Float
    @JvmField
	var b: Float
    @JvmField
	var c: Float
    @JvmField
	var d: Float
    @JvmField
	var e: Float
    @JvmField
	var f: Float


    constructor(a: Float = 1f, b: Float = 0f, c: Float = 0f, d: Float = 1f, e: Float = 0f, f: Float = 0f) {
        this.a = a
        this.b = b
        this.c = c
        this.d = d
        this.e = e
        this.f = f
    }

    constructor(a: Float) : this(a, 0f, 0f, 0f, 0f, 0f) {
        this.a = a
        this.d = a
    }
    constructor(copy: Matrix) : this(copy.a, copy.b, copy.c, copy.d, copy.e, copy.f) {}
    constructor(one: Matrix, two: Matrix) {
        a = one.a * two.a + one.b * two.c
        b = one.a * two.b + one.b * two.d
        c = one.c * two.a + one.d * two.c
        d = one.c * two.b + one.d * two.d
        e = one.e * two.a + one.f * two.c + two.e
        f = one.e * two.b + one.f * two.d + two.f
    }

    fun concat(m: Matrix): Matrix {
        val a = a * m.a + b * m.c
        val b = this.a * m.b + b * m.d
        val c = c * m.a + d * m.c
        val d = this.c * m.b + d * m.d
        val e = e * m.a + f * m.c + m.e
        f = this.e * m.b + f * m.d + m.f
        this.a = a
        this.b = b
        this.c = c
        this.d = d
        this.e = e
        return this
    }

    fun scale(sx: Float, sy: Float): Matrix {
        a *= sx
        b *= sx
        c *= sy
        d *= sy
        return this
    }

    fun scale(s: Float): Matrix {
        return scale(s, s)
    }

    fun translate(tx: Float, ty: Float): Matrix {
        e += tx * a + ty * c
        f += tx * b + ty * d
        return this
    }

    fun rotate(degrees: Float): Matrix {
        var degrees = degrees
        while (degrees < 0) degrees += 360f
        while (degrees >= 360) degrees -= 360f
        if (Math.abs(0 - degrees) < 0.0001) {
            // Nothing to do
        } else if (Math.abs(90 - degrees) < 0.0001) {
            val save_a = a
            val save_b = b
            a = c
            b = d
            c = -save_a
            d = -save_b
        } else if (Math.abs(180 - degrees) < 0.0001) {
            a = -a
            b = -b
            c = -c
            d = -d
        } else if (Math.abs(270 - degrees) < 0.0001) {
            val save_a = a
            val save_b = b
            a = -c
            b = -d
            c = save_a
            d = save_b
        } else {
            val sin = Math.sin(degrees * Math.PI / 180.0).toFloat()
            val cos = Math.cos(degrees * Math.PI / 180.0).toFloat()
            val save_a = a
            val save_b = b
            a = cos * save_a + sin * c
            b = cos * save_b + sin * d
            c = -sin * save_a + cos * c
            d = -sin * save_b + cos * d
        }
        return this
    }

    override fun toString(): String {
        return "[$a $b $c $d $e $f]"
    }

    companion object {
        fun Identity(): Matrix {
            return Matrix(1f, 0f, 0f, 1f, 0f, 0f)
        }

        fun Scale(x: Float): Matrix {
            return Matrix(x, 0f, 0f, x, 0f, 0f)
        }

        fun Scale(x: Float, y: Float): Matrix {
            return Matrix(x, 0f, 0f, y, 0f, 0f)
        }

        fun Translate(x: Float, y: Float): Matrix {
            return Matrix(1f, 0f, 0f, 1f, x, y)
        }

        fun Rotate(degrees: Float): Matrix {
            var degrees = degrees
            val sin: Float
            val cos: Float
            while (degrees < 0) degrees += 360f
            while (degrees >= 360) degrees -= 360f
            if (Math.abs(0 - degrees) < 0.0001) {
                sin = 0f
                cos = 1f
            } else if (Math.abs(90 - degrees) < 0.0001) {
                sin = 1f
                cos = 0f
            } else if (Math.abs(180 - degrees) < 0.0001) {
                sin = 0f
                cos = -1f
            } else if (Math.abs(270 - degrees) < 0.0001) {
                sin = -1f
                cos = 0f
            } else {
                sin = Math.sin(degrees * Math.PI / 180.0).toFloat()
                cos = Math.cos(degrees * Math.PI / 180.0).toFloat()
            }
            return Matrix(cos, sin, -sin, cos, 0f, 0f)
        }
    }
}