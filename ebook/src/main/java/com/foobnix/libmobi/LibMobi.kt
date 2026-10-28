package com.foobnix.libmobi

import br.com.ebook.foobnix.sys.NativeLibLoader

object LibMobi {
    init {
        NativeLibLoader.loadLibrary("mobi")
    }

    @JvmStatic
    external fun convertToEpub(input: String?, output: String?): Int
}