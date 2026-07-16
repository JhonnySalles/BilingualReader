package br.com.ebook.foobnix.sys

import org.ebookdroid.BookType
import br.com.ebook.foobnix.pdf.info.ExtUtils
import java.util.concurrent.locks.ReentrantLock

class TempHolder {

    @Volatile
    var path: String? = null
    
    var isTextFormat: Boolean = false
    var isTextFormatButNotTxt: Boolean = false

    var login = ""
    var password = ""
    var linkPage = -1
    var timerFinishTime = 0L
    var pageDelta = 0

    @Volatile
    var loadingCancelled = false
    var forceAppLang = false

    @Volatile
    var lastRecycledDocument = 0L

    fun init(pathI: String) {
        path = pathI
        isTextFormat = isTextFormatInner()
        isTextFormatButNotTxt = isTextFormatButNotTxt()
    }

    fun clear() {
        path = null
    }

    private fun isTextFormatInner(): Boolean {
        return try {
            ExtUtils.isTextFomat(path)
        } catch (e: Exception) {
            false
        }
    }

    private fun isTextFormatButNotTxt(): Boolean {
        return try {
            ExtUtils.isTextFomat(path) && !BookType.TXT.is(path)
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        @JvmField
        val lock = ReentrantLock()
        
        @JvmField
        val inst = TempHolder()

        @JvmStatic
        fun get(): TempHolder = inst

        @JvmField
        var listHash = 0

        @Volatile
        @JvmField
        var isSearching = false

        @Volatile
        @JvmField
        var isConverting = false

        @Volatile
        @JvmField
        var isRecordTTS = false
    }
}
