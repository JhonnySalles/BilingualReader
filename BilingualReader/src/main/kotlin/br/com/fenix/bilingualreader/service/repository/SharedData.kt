package br.com.fenix.bilingualreader.service.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.fenix.bilingualreader.model.entity.Chapters
import br.com.fenix.bilingualreader.service.listener.ChapterLoadListener
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse

class SharedData {
    companion object {
        private var mDocument: DocumentParse? = null
        fun getDocumentParse(): DocumentParse? = mDocument
        fun setDocumentParse(document: DocumentParse?) {
            mDocument = document
        }

        private var mListener: MutableList<ChapterLoadListener> = mutableListOf()
        private var mProcessed: Any? = null
        private var mListChapters: MutableLiveData<List<Chapters>> = MutableLiveData(arrayListOf())
        val chapters: LiveData<List<Chapters>> = mListChapters

        fun isProcessed(obj: Any?) : Boolean = obj != mProcessed || mListChapters.value!!.isEmpty()
        fun isImageNull() : Boolean = mListChapters.value!!.any { it.image == null }

        fun clearChapters() {
            mProcessed = null
            mListChapters.value = arrayListOf()
        }

        fun setChapters(obj: Any?, chapters: List<Chapters>) {
            mProcessed = obj
            mListChapters.value = chapters
        }

        fun selectPage(page: Int) {
            if (mListChapters.value == null || mListChapters.value!!.isEmpty())
                return

            mListChapters.value?.forEach { it.isSelected = it.page == page }
            mListChapters.value = mListChapters.value
        }

        fun addListener(listener: ChapterLoadListener) {
            mListener.add(listener)
        }

        fun remListener(listener: ChapterLoadListener) {
            mListener.remove(listener)
        }

        fun callListeners(page: Int) {
            for (listener in mListener)
                listener.onLoading(page)
        }
    }
}