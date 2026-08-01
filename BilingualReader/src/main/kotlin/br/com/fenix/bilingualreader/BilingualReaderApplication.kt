package br.com.fenix.bilingualreader

import android.app.Application
import br.com.fenix.bilingualreader.service.services.BookCoverFetcher
import br.com.fenix.bilingualreader.service.services.MangaCoverFetcher
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
            
            // Ignorar erros 504 (Gateway Timeout) originados por requisições HTTP para a API de terceiros
            options.beforeSend = io.sentry.SentryOptions.BeforeSendCallback { event, _ ->
                val exception = event.exceptions?.firstOrNull()
                if (exception?.type == "SentryHttpClientException" && exception.value?.contains("504") == true) {
                    return@BeforeSendCallback null
                }
                event
            }
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
