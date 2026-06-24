package br.com.fenix.bilingualreader.service.parses.manga

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
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import kotlinx.coroutines.withContext
import java.io.File

class MangaCoverFetcher(
    private val manga: Manga,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val context = options.context
        val controller = MangaImageCoverController.instance

        val file = withContext(MangaImageCoverController.thread) {
            controller.getMangaCoverFile(context, manga, true)
        } ?: return null

        return SourceResult(
            source = ImageSource(file = file.toOkioPath(), fileSystem = FileSystem.SYSTEM),
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    class Factory : Fetcher.Factory<Manga> {
        override fun create(data: Manga, options: Options, imageLoader: ImageLoader): Fetcher {
            return MangaCoverFetcher(data, options)
        }
    }
}
