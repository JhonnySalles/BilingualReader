package br.com.fenix.bilingualreader.view.managers

import android.os.Handler
import android.os.Looper
import android.os.Message
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.service.scanner.ScannerManga
import java.lang.ref.WeakReference

class MangaScannerHandler(scanner: ScannerManga, private val library: Library) : Handler(Looper.getMainLooper()) {
    private val mScannerRef: WeakReference<ScannerManga> = WeakReference(scanner)

    override fun handleMessage(msg: Message) {
        mScannerRef.get()?.scanLibrary(library)
    }
}
