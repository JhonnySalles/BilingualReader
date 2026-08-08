package br.com.fenix.bilingualreader.service.update

import android.content.Context
import br.com.fenix.bilingualreader.service.listener.ApiListener
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UpdateAppTest {

    @Test
    fun `consult should trigger failure callback when credential assets missing`() {
        val context = mockk<Context>(relaxed = true)
        val updateApp = UpdateApp(context)
        val listener = mockk<ApiListener<Releases>>(relaxed = true)

        updateApp.consult(listener)

        verify { listener.onFailure(any()) }
    }

    @Test
    fun `UpdateApp instantiation should preserve context`() {
        val context = mockk<Context>(relaxed = true)
        val updateApp = UpdateApp(context)

        assertNotNull(updateApp.mContext)
    }
}
