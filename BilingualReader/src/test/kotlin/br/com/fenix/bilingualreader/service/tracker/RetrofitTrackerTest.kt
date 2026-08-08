package br.com.fenix.bilingualreader.service.tracker

import br.com.fenix.bilingualreader.service.tracker.mal.MyAnimeListService
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RetrofitTrackerTest {

    @Test
    fun `getService should return Retrofit service implementation`() {
        val service = RetrofitTracker.getService(MyAnimeListService::class.java)
        assertNotNull(service)
    }

    @Test
    fun `getOAuth should return OAuth Retrofit service implementation`() {
        val oauthService = RetrofitTracker.getOAuth(MyAnimeListService::class.java)
        assertNotNull(oauthService)
    }
}
