package br.com.ebook.fitz

class ColorSpace private constructor(private var pointer: Long) {
    protected external fun finalize()
    fun destroy() {
        finalize()
        pointer = 0
    }

    val numberOfComponents: Int
        external get

    override fun toString(): String {
        if (this === DeviceGray) return "DeviceGray"
        if (this === DeviceRGB) return "DeviceRGB"
        if (this === DeviceBGR) return "DeviceBGR"
        return if (this === DeviceCMYK) "DeviceCMYK" else "ColorSpace(" + numberOfComponents + ")"
    }

    companion object {
        init {
            Context.init()
        }

        private external fun nativeDeviceGray(): Long
        private external fun nativeDeviceRGB(): Long
        private external fun nativeDeviceBGR(): Long
        private external fun nativeDeviceCMYK(): Long
        protected fun fromPointer(p: Long): ColorSpace {
            if (p == DeviceGray.pointer) return DeviceGray
            if (p == DeviceRGB.pointer) return DeviceRGB
            if (p == DeviceBGR.pointer) return DeviceBGR
            return if (p == DeviceCMYK.pointer) DeviceCMYK else ColorSpace(
                p
            )
        }

        var DeviceGray = ColorSpace(nativeDeviceGray())
        var DeviceRGB = ColorSpace(nativeDeviceRGB())
        var DeviceBGR = ColorSpace(nativeDeviceBGR())
        var DeviceCMYK = ColorSpace(nativeDeviceCMYK())
    }
}