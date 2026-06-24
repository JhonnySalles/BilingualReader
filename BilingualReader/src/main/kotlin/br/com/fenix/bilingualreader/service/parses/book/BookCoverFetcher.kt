package br.com.fenix.bilingualreader.service.parses.book

import android.content.Context
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import kotlinx.coroutines.withContext
import java.io.File

class BookCoverFetcher(
    private val book: Book,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val context = options.context
        val controller = BookImageCoverController.instance

        val file = withContext(BookImageCoverController.thread) {
            controller.getBookCoverFile(context, book, true)
        } ?: return null

        return SourceResult(
            source = ImageSource(file = file.toOkioPath(), fileSystem = FileSystem.SYSTEM),
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<Book> {
        override fun create(data: Book, options: Options, imageLoader: ImageLoader): Fetcher {
            return BookCoverFetcher(data, options)
        }
    }
}
