package br.com.fenix.bilingualreader.service.update

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RetrofitUpdateTest {

    @Test
    fun `getService should return AppDistributionService implementation`() {
        val service = RetrofitUpdate.getService(AppDistributionService::class.java)
        assertNotNull(service)
    }
}
