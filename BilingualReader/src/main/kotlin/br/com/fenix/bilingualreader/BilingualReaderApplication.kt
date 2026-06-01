package br.com.fenix.bilingualreader

import android.app.Application
import com.google.firebase.FirebaseApp

class BilingualReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
