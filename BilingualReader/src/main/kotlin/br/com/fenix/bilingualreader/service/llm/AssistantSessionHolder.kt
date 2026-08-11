package br.com.fenix.bilingualreader.service.llm

import br.com.fenix.bilingualreader.model.enums.Languages
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import br.com.fenix.bilingualreader.service.parses.manga.Parse

/**
 * Holds non-parcelable reader state for [br.com.fenix.bilingualreader.view.ui.assistant.ReadingAssistantActivity].
 */
object AssistantSessionHolder {
    var type: Type = Type.BOOK
    var title: String = ""
    var page: Int = 0
    var chapter: Int = 0
    var bookId: Long? = null
    var mangaId: Long? = null
    var userLanguage: Languages = Languages.PORTUGUESE
    var ocrLanguage: Languages? = null
    var bookLanguage: Languages? = null
    var preloadSummary: String? = null
    var bookParse: DocumentParse? = null
    var mangaParse: Parse? = null
    var selectedOpenRouterModel: String? = null

    fun clear() {
        bookParse = null
        mangaParse = null
        preloadSummary = null
        title = ""
        page = 0
        chapter = 0
        bookId = null
        mangaId = null
        ocrLanguage = null
        bookLanguage = null
        selectedOpenRouterModel = null
    }
}
