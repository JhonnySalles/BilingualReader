package br.com.fenix.bilingualreader.service.ocr

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class GoogleVisionTest {

    @Test
    fun instanceCanBeCreated() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val googleVision = GoogleVision.getInstance(context)
        assertNotNull(googleVision)
    }
}
