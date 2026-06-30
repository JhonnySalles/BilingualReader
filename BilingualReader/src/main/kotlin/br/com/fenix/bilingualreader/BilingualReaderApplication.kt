package br.com.fenix.bilingualreader

import android.app.Application
import br.com.fenix.bilingualreader.service.parses.book.BookCoverFetcher
import br.com.fenix.bilingualreader.service.parses.manga.MangaCoverFetcher
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.firebase.FirebaseApp

class BilingualReaderApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        io.sentry.android.core.SentryAndroid.init(this) { options ->
            options.isDebug = BuildConfig.DEBUG
            options.tracesSampleRate = 1.0
        }
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
