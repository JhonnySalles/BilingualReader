package br.com.ebook.fitz

// This class handles the loading of the MuPDF shared library, together
// with the ThreadLocal magic to get the required context.
//
// The only publicly accessible method here is Context.setStoreSize, which
// sets the store size to use. This must be called before any other MuPDF
// function.
object Context {
    // Make sure to initialize inited before calling
    // init() from the static block below.
    private var inited = false

    init {
        init()
    }

    private external fun initNative(): Int
    external fun gprfSupportedNative(): Int
    @JvmStatic
	fun init() {
        if (true) {
            return
        }
        if (!inited) {
            inited = true
            try {
                System.loadLibrary("mupdf_java")
            } catch (e: UnsatisfiedLinkError) {
                try {
                    System.loadLibrary("mupdf_java64")
                } catch (ee: UnsatisfiedLinkError) {
                    System.loadLibrary("mupdf_java32")
                }
            }
            if (initNative() < 0) throw RuntimeException("cannot initialize mupdf library")
        }
    } // FIXME: We should support the store size being changed dynamically.
    // This requires changes within the MuPDF core.
    //public native static void setStoreSize(long newSize);
}