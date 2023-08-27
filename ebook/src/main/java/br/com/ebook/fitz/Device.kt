package br.com.ebook.fitz

import br.com.ebook.fitz.Context.init

open class Device {
    protected var pointer: Long
    protected open external fun finalize()
    open fun destroy() {
        finalize()
        pointer = 0
    }

    private external fun newNative(): Long

    protected constructor() {
        pointer = newNative()
    }

    protected constructor(p: Long) {
        pointer = p
    }

    /* To implement your own device in Java, you should define your own
	 * class that extends Device, and override as many of the following
	 * functions as is appropriate. For example:
	 *
	 * class ImageTraceDevice extends Device
	 * {
	 *	void fillImage(Image img, Matrix ctx, float alpha) {
	 *		System.out.println("Image!");
	 *	}
	 * };
	 */
    open fun close() {}
    fun fillPath(path: Path?, evenOdd: Boolean, ctm: Matrix?, cs: ColorSpace?, color: FloatArray?, alpha: Float) {}
    fun strokePath(path: Path?, stroke: StrokeState?, ctm: Matrix?, cs: ColorSpace?, color: FloatArray?, alpha: Float) {}
    open fun clipPath(path: Path?, evenOdd: Boolean, ctm: Matrix?) {}
    open fun clipStrokePath(path: Path?, stroke: StrokeState?, ctm: Matrix?) {}
    fun fillText(text: Text?, ctm: Matrix?, cs: ColorSpace?, color: FloatArray?, alpha: Float) {}
    fun strokeText(text: Text?, stroke: StrokeState?, ctm: Matrix?, cs: ColorSpace?, color: FloatArray?, alpha: Float) {}
    open fun clipText(text: Text?, ctm: Matrix?) {}
    open fun clipStrokeText(text: Text?, stroke: StrokeState?, ctm: Matrix?) {}
    open fun ignoreText(text: Text?, ctm: Matrix?) {}
    fun fillShade(shd: Shade?, ctm: Matrix?, alpha: Float) {}
    fun fillImage(img: Image?, ctm: Matrix?, alpha: Float) {}
    fun fillImageMask(img: Image?, ctm: Matrix?, cs: ColorSpace?, color: FloatArray?, alpha: Float) {}
    open fun clipImageMask(img: Image?, ctm: Matrix?) {}
    open fun popClip() {}
    fun beginMask(area: Rect?, luminosity: Boolean, cs: ColorSpace?, bc: FloatArray?) {}
    open fun endMask() {}
    fun beginGroup(area: Rect?, cs: ColorSpace?, isolated: Boolean, knockout: Boolean, blendmode: Int, alpha: Float) {}
    open fun endGroup() {}
    open fun beginTile(area: Rect?, view: Rect?, xstep: Float, ystep: Float, ctm: Matrix?, id: Int): Int {
        return 0
    }

    open fun endTile() {}

    companion object {
        init {
            init()
        }

        /* Flags */
        const val FLAG_MASK = 1
        const val FLAG_COLOR = 2
        const val FLAG_UNCACHEABLE = 4
        const val FLAG_FILLCOLOR_UNDEFINED = 8
        const val FLAG_STROKECOLOR_UNDEFINED = 16
        const val FLAG_STARTCAP_UNDEFINED = 32
        const val FLAG_DASHCAP_UNDEFINED = 64
        const val FLAG_ENDCAP_UNDEFINED = 128
        const val FLAG_LINEJOIN_UNDEFINED = 256
        const val FLAG_MITERLIMIT_UNDEFINED = 512
        const val FLAG_LINEWIDTH_UNDEFINED = 1024

        /* PDF 1.4 -- standard separable */
        const val BLEND_NORMAL = 0
        const val BLEND_MULTIPLY = 1
        const val BLEND_SCREEN = 2
        const val BLEND_OVERLAY = 3
        const val BLEND_DARKEN = 4
        const val BLEND_LIGHTEN = 5
        const val BLEND_COLOR_DODGE = 6
        const val BLEND_COLOR_BURN = 7
        const val BLEND_HARD_LIGHT = 8
        const val BLEND_SOFT_LIGHT = 9
        const val BLEND_DIFFERENCE = 10
        const val BLEND_EXCLUSION = 11

        /* PDF 1.4 -- standard non-separable */
        const val BLEND_HUE = 12
        const val BLEND_SATURATION = 13
        const val BLEND_COLOR = 14
        const val BLEND_LUMINOSITY = 15

        /* For packing purposes */
        const val BLEND_MODEMASK = 15
        const val BLEND_ISOLATED = 16
        const val BLEND_KNOCKOUT = 32

        /* Device hints */
        const val IGNORE_IMAGE = 1
        const val IGNORE_SHADE = 2
    }
}