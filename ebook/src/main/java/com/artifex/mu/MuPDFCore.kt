package com.artifex.mu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF

class MuPDFCore {
    /* Readable members */
    private var numPages = -1
    private var pageWidth = 0f
    private var pageHeight = 0f
    private var globals: Long
    private lateinit var fileBuffer: ByteArray
    private var file_format: String
    private var isUnencryptedPDF: Boolean
    private val wasOpenedFromBuffer: Boolean
    private external fun openFile(filename: String): Long
    private external fun openBuffer(magic: String): Long
    private external fun fileFormatInternal(): String
    private val isUnencryptedPDFInternal: Boolean
        private external get

    private external fun countPagesInternal(): Int
    private external fun gotoPageInternal(localActionPageNum: Int)
    private external fun getPageWidth(): Float
    private external fun getPageHeight(): Float
    private external fun drawPage(
        bitmap: Bitmap,
        pageW: Int, pageH: Int,
        patchX: Int, patchY: Int,
        patchW: Int, patchH: Int,
        cookiePtr: Long
    )

    private external fun updatePageInternal(
        bitmap: Bitmap,
        page: Int,
        pageW: Int, pageH: Int,
        patchX: Int, patchY: Int,
        patchW: Int, patchH: Int,
        cookiePtr: Long
    )

    private external fun searchPage(text: String?): Array<RectF>
    private external fun text(): Array<Array<Array<Array<TextChar>>>>
    private external fun textAsHtml(): ByteArray
    private external fun addMarkupAnnotationInternal(quadPoints: Array<PointF>, type: Int)
    private external fun addInkAnnotationInternal(arcs: Array<Array<PointF>>)
    private external fun deleteAnnotationInternal(annot_index: Int)
    private external fun passClickEventInternal(page: Int, x: Float, y: Float): Int
    private external fun setFocusedWidgetChoiceSelectedInternal(selected: Array<String>)
    private val focusedWidgetChoiceSelected: Array<String?>?
        private external get
    private val focusedWidgetChoiceOptions: Array<String?>?
        private external get
    private val focusedWidgetSignatureState: Int
        private external get

    private external fun checkFocusedSignatureInternal(): String?
    private external fun signFocusedSignatureInternal(keyFile: String, password: String): Boolean
    private external fun setFocusedWidgetTextInternal(text: String): Int
    private external fun getWidgetAreasInternal(page: Int): Array<RectF?>?
    private external fun getAnnotationsInternal(page: Int): Array<Annotation?>?

    //private native OutlineItem [] getOutlineInternal();
    private external fun hasOutlineInternal(): Boolean
    private external fun needsPasswordInternal(): Boolean
    private external fun authenticatePasswordInternal(password: String): Boolean
    private external fun startAlertsInternal()
    private external fun stopAlertsInternal()
    private external fun destroying()
    private external fun hasChangesInternal(): Boolean
    private external fun saveInternal()
    private external fun createCookie(): Long
    private external fun destroyCookie(cookie: Long)
    private external fun abortCookie(cookie: Long)
    private external fun startProofInternal(resolution: Int): String?
    private external fun endProofInternal(filename: String)
    private external fun getNumSepsOnPageInternal(page: Int): Int
    private external fun controlSepOnPageInternal(page: Int, sep: Int, disable: Boolean): Int
    private external fun getSepInternal(page: Int, sep: Int): Separation?
    external fun javascriptSupported(): Boolean
    inner class Cookie {
        private val cookiePtr: Long

        init {
            cookiePtr = createCookie()
            if (cookiePtr == 0L) throw OutOfMemoryError()
        }

        fun abort() {
            abortCookie(cookiePtr)
        }

        fun destroy() {
            // We could do this in finalize, but there's no guarantee that
            // a finalize will occur before the muPDF context occurs.
            destroyCookie(cookiePtr)
        }
    }

    constructor(context: Context?, filename: String) {
        globals = openFile(filename)
        if (globals == 0L) {
            throw Exception(String.format("Não é possível abrir o item selecionado", filename))
        }
        file_format = fileFormatInternal()
        isUnencryptedPDF = isUnencryptedPDFInternal
        wasOpenedFromBuffer = false
    }

    constructor(context: Context?, buffer: ByteArray, magic: String?) {
        fileBuffer = buffer
        globals = openBuffer(magic ?: "")
        if (globals == 0L) {
            throw Exception("Não é possível abrir o buffer")
        }
        file_format = fileFormatInternal()
        isUnencryptedPDF = isUnencryptedPDFInternal
        wasOpenedFromBuffer = true
    }

    fun countPages(): Int {
        if (numPages < 0) numPages = countPagesSynchronized()
        return numPages
    }

    @Synchronized
    private fun countPagesSynchronized(): Int {
        return countPagesInternal()
    }

    /* Shim function */
    private fun gotoPage(page: Int) {
        var page = page
        if (page > numPages - 1) page = numPages - 1 else if (page < 0) page = 0
        gotoPageInternal(page)
        pageWidth = getPageWidth()
        pageHeight = getPageHeight()
    }

    @Synchronized
    fun onDestroy() {
        destroying()
        globals = 0
    }

    @Synchronized
    fun searchPage(page: Int, text: String?): Array<RectF> {
        gotoPage(page)
        return searchPage(text)
    }

    @Synchronized
    fun html(page: Int): ByteArray {
        gotoPage(page)
        return textAsHtml()
    }

    @Synchronized
    fun textLines(page: Int): Array<Array<TextWord>> {
        gotoPage(page)
        val chars = text()

        // The text of the page held in a hierarchy (blocks, lines, spans).
        // Currently we don't need to distinguish the blocks level or
        // the spans, and we need to collect the text into words.
        val lns = ArrayList<Array<TextWord>>()
        for (bl in chars) {
            if (bl == null) continue
            for (ln in bl) {
                val wds = ArrayList<TextWord>()
                var wd = TextWord()
                for (sp in ln) {
                    for (tc in sp) {
                        if (tc.c != ' ') {
                            wd.Add(tc)
                        } else if (wd.w.length > 0) {
                            wds.add(wd)
                            wd = TextWord()
                        }
                    }
                }
                if (wd.w.length > 0) wds.add(wd)
                if (wds.size > 0) lns.add(wds.toTypedArray())
            }
        }
        return lns.toTypedArray()
    }

    @Synchronized
    fun save() {
        saveInternal()
    }

    companion object {
        /* load our native library */
        private var gs_so_available = false

        init {
            println("Loading dll")
            System.loadLibrary("mupdf_java")
            println("Loaded dll")
            if (gprfSupportedInternal()) {
                try {
                    System.loadLibrary("gs")
                    gs_so_available = true
                } catch (e: UnsatisfiedLinkError) {
                    gs_so_available = false
                }
            }
        }

        /* The native functions */
        private external fun gprfSupportedInternal(): Boolean
    }
}