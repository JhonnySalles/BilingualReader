package br.com.fenix.bilingualreader.service.tracker.mal

import android.content.Context
import br.com.fenix.bilingualreader.service.listener.ApiListener
import br.com.fenix.bilingualreader.service.tracker.RetrofitTracker
import br.com.fenix.bilingualreader.util.secrets.Secrets
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MyAnimeListTrackerTest {

    private lateinit var context: Context
    private lateinit var tracker: MyAnimeListTracker
    private val service: MyAnimeListService = mockk(relaxed = true)
    private val secrets: Secrets = mockk(relaxed = true)

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        
        mockkObject(RetrofitTracker.Companion)
        every { RetrofitTracker.getService(MyAnimeListService::class.java) } returns service
        every { RetrofitTracker.getOAuth(MyAnimeListService::class.java) } returns service
        
        mockkObject(Secrets.Instance)
        every { Secrets.getSecrets(any()) } returns secrets
        every { secrets.getMyAnimeListClientId() } returns "test_client_id"
        
        tracker = MyAnimeListTracker(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `login should call service and notify listener on success`() {
        val email = "test@test.com"
        val password = "password"
        val listener = mockk<ApiListener<OAuth>>(relaxed = true)
        val oauth = OAuth("refresh", "access", "bearer", expires_in = 3600)
        
        val call = mockk<Call<OAuth>>()
        every { service.login(any(), any(), any(), any()) } returns call
        
        every { call.enqueue(any()) } answers {
            val callback = firstArg<Callback<OAuth>>()
            callback.onResponse(call, Response.success(oauth))
        }

        tracker.login(email, password, listener)

        // Maps to Service.login(client_id = email, code = password, ...)
        verify { service.login(email, password, any(), any()) }
        verify { listener.onSuccess(oauth) }
    }

    @Test
    fun `getListManga should call service`() {
        val search = "One Piece"
        val listener = mockk<ApiListener<List<MalMangaDetail>>>(relaxed = true)
        val call = mockk<Call<MalMangaList>>(relaxed = true)
        
        every { service.getListManga(any(), any(), any(), any(), any(), any()) } returns call
        
        tracker.getListManga(search, listener)
        
        verify { service.getListManga(null, any(), search, limit = 20, any(), any()) }
    }
}
