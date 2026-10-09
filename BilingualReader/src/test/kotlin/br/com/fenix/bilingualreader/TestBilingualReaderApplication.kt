package br.com.fenix.bilingualreader

import android.app.Application
import br.com.fenix.bilingualreader.util.helpers.Telemetry

/**
 * Custom Application class automatically used by Robolectric in tests
 * instead of [BilingualReaderApplication]. Disables Telemetry (Sentry / Crashlytics)
 * during all test runs to avoid polluting production error monitoring.
 */
class TestBilingualReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Telemetry.isEnabled = false
    }
}
