package br.com.ebook.foobnix.sys

import br.com.ebook.foobnix.pdf.info.ExtUtils
import org.ebookdroid.BookType
import java.util.concurrent.locks.ReentrantLock

class TempHolder {

    @JvmField
    @Volatile
    var path: String? = null
    
    @JvmField
    var isTextFormat: Boolean = false
    
    @JvmField
    var isTextFormatButNotTxt: Boolean = false

    @JvmField
    var login = ""
    
    @JvmField
    var password = ""
    
    @JvmField
    var linkPage = -1
    
    @JvmField
    var timerFinishTime = 0L
    
    @JvmField
    var pageDelta = 0

    @JvmField
    @Volatile
    var loadingCancelled = false
    
    @JvmField
    var forceAppLang = false

    @JvmField
    @Volatile
    var lastRecycledDocument = 0L

    fun init(pathI: String) {
        path = pathI
        isTextFormat = isTextFormatInner()
        isTextFormatButNotTxt = checkIsTextFormatButNotTxt()
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

    private fun checkIsTextFormatButNotTxt(): Boolean {
        return try {
            ExtUtils.isTextFomat(path) && !BookType.TXT.`is`(path)
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
