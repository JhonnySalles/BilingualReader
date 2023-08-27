package br.com.ebook.fitz

import br.com.ebook.fitz.Context.init

class NativeDevice protected constructor(p: Long) : Device(p) {
    private var nativeInfo: Long = 0
    private var nativeResource: Any? = null
    external override fun finalize()
    override fun destroy() {
        super.destroy()
        nativeInfo = 0
        nativeResource = null
    }

    external override fun close()
    external fun fillPath(path: Path?, evenOdd: Boolean, ctm: Matrix?, cs: ColorSpace?, color: FloatArray?, alpha: Float, cp: Int)
    external fun strokePath(path: Path?, stroke: StrokeState?, ctm: Matrix?, cs: ColorSpace?, color: FloatArray?, alpha: Float, cp: Int)
    external override fun clipPath(path: Path?, evenOdd: Boolean, ctm: Matrix?)
    external override fun clipStrokePath(path: Path?, stroke: StrokeState?, ctm: Matrix?)
    external fun fillText(text: Text?, ctm: Matrix?, cs: ColorSpace?, color: FloatArray?, alpha: Float, cp: Int)
    external fun strokeText(text: Text?, stroke: StrokeState?, ctm: Matrix?, cs: ColorSpace?, color: FloatArray?, alpha: Float, cp: Int)
    external override fun clipText(text: Text?, ctm: Matrix?)
    external override fun clipStrokeText(text: Text?, stroke: StrokeState?, ctm: Matrix?)
    external override fun ignoreText(text: Text?, ctm: Matrix?)
    external fun fillShade(shd: Shade?, ctm: Matrix?, alpha: Float, cp: Int)
    external fun fillImage(img: Image?, ctm: Matrix?, alpha: Float, cp: Int)
    external fun fillImageMask(img: Image?, ctm: Matrix?, cs: ColorSpace?, color: FloatArray?, alpha: Float, cp: Int)

    /* FIXME: Why no scissor? */
    external override fun clipImageMask(img: Image?, ctm: Matrix?)
    external override fun popClip()
    external fun beginMask(rect: Rect?, luminosity: Boolean, cs: ColorSpace?, bc: FloatArray?, cp: Int)
    external override fun endMask()
    external fun beginGroup(rect: Rect?, isolated: Boolean, knockout: Boolean, blendmode: Int, alpha: Float)
    external override fun endGroup()
    external override fun beginTile(area: Rect?, view: Rect?, xstep: Float, ystep: Float, ctm: Matrix?, id: Int): Int
    external override fun endTile()

    companion object {
        init {
            init()
        }
    }
}