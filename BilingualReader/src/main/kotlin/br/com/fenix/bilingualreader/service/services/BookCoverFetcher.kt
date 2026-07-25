package br.com.fenix.bilingualreader.service.services

import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toOkioPath

class BookCoverFetcher(
    private val book: Book,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val context = options.context
        val controller = BookImageCoverController.Companion.instance

        val file = withContext(BookImageCoverController.Companion.thread) {
            controller.getBookCoverFile(context, book, true)
        } ?: return null

        return SourceResult(
            source = ImageSource(file = file.toOkioPath(), fileSystem = FileSystem.Companion.SYSTEM),
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