package br.com.ebook.fitz

interface PathWalker {
    fun moveTo(x: Float, y: Float)
    fun lineTo(x: Float, y: Float)
    fun curveTo(cx1: Float, cy1: Float, cx2: Float, cy2: Float, ex: Float, ey: Float)
    fun closePath()
}