package br.com.fenix.bilingualreader

import android.app.Application
import com.google.firebase.FirebaseApp
import coil.ImageLoader
import coil.ImageLoaderFactory
import br.com.fenix.bilingualreader.service.parses.manga.MangaCoverFetcher
import br.com.fenix.bilingualreader.service.parses.book.BookCoverFetcher

class BilingualReaderApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .allowHardware(false)
            .components {
                add(MangaCoverFetcher.Factory())
                add(BookCoverFetcher.Factory())
            }
            .build()
    }
}
