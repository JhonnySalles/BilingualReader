package br.com.fenix.bilingualreader.view.ui.assistant

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.service.parses.book.DocumentParse
import kotlinx.coroutines.CoroutineScope

/**
 * Backward-compatible entry for book chapter summaries.
 * Prefer [PopupReadingSummary.showBook].
 */
object PopupChapterSummary {

    fun show(
        context: Context,
        scope: CoroutineScope,
        book: Book,
        parse: DocumentParse,
        currentPage0Based: Int
    ) {
        PopupReadingSummary.showBook(context, scope, book, parse, currentPage0Based)
    }
}
