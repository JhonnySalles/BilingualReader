package br.com.fenix.bilingualreader.service.sharemark

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.File
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import java.time.LocalDateTime
import br.com.fenix.bilingualreader.model.entity.mock.MangaMock
import br.com.fenix.bilingualreader.model.enums.ShareMarkType
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.service.repository.HistoryRepository
import br.com.fenix.bilingualreader.service.repository.MangaAnnotationRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.FileList
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Collections
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ShareMarkGDriveTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var controller: ShareMarkGDriveController

    // Google API Mocks
    private val drive = mockk<Drive>(relaxed = true)
    private val driveFiles = mockk<Drive.Files>(relaxed = true)
    private val driveList = mockk<Drive.Files.List>(relaxed = true)
    private val googleSignInAccount = mockk<GoogleSignInAccount>(relaxed = true)
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk<Context>(relaxed = true)
        val cm = mockk<ConnectivityManager>(relaxed = true)
        val capabilities = mockk<NetworkCapabilities>(relaxed = true)
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns cm
        every { cm.activeNetwork } returns mockk()
        every { cm.getNetworkCapabilities(any()) } returns capabilities
        every { capabilities.hasTransport(any()) } returns true
        
        controller = spyk(ShareMarkGDriveController(context), recordPrivateCalls = true)

        mockkStatic(GoogleSignIn::class)
        mockkStatic(AndroidHttp::class)
        mockkStatic(JacksonFactory::class)
        mockkStatic(GoogleAccountCredential::class)
        mockkStatic(Dispatchers::class)

        every { GoogleSignIn.getLastSignedInAccount(any()) } returns googleSignInAccount
        every { AndroidHttp.newCompatibleTransport() } returns mockk()
        every { JacksonFactory.getDefaultInstance() } returns mockk()
        every { GoogleAccountCredential.usingOAuth2(any(), any()) } returns mockk(relaxed = true)
        every { Dispatchers.IO } returns testDispatcher

        mockkConstructor(Drive.Builder::class)
        val builderMock = mockk<Drive.Builder>(relaxed = true)
        every { anyConstructed<Drive.Builder>().setApplicationName(any()) } returns builderMock
        every { builderMock.setApplicationName(any()) } returns builderMock
        every { anyConstructed<Drive.Builder>().build() } returns drive
        every { builderMock.build() } returns drive

        every { drive.files() } returns driveFiles
        every { driveFiles.list() } returns driveList
        
        val driveCreate = mockk<Drive.Files.Create>(relaxed = true)
        val driveUpdate = mockk<Drive.Files.Update>(relaxed = true)
        val driveFile = com.google.api.services.drive.model.File().setId("mock_id")
        
        every { driveFiles.create(any()) } returns driveCreate
        every { driveFiles.create(any(), any()) } returns driveCreate
        every { driveCreate.setFields(any()) } returns driveCreate
        every { driveCreate.execute() } returns driveFile
        
        every { driveFiles.update(any(), any()) } returns driveUpdate
        every { driveFiles.update(any(), any(), any()) } returns driveUpdate
        every { driveUpdate.execute() } returns driveFile
        
        every { driveFiles.get(any()) } returns mockk(relaxed = true)
        
        mockkObject(GeneralConsts.Companion)
        every { GeneralConsts.getCacheDir(any()) } returns File("BilingualReader/build/tmp/test_cache")
        
        // Mock SharedPreferences
        val prefs = mockk<android.content.SharedPreferences>(relaxed = true)
        every { GeneralConsts.getSharedPreferences(any()) } returns prefs
        every { prefs.getString(any(), any()) } returns "2000-01-01T01:01:01.001-0300"
        
        mockkConstructor(MangaRepository::class)
        mockkConstructor(HistoryRepository::class)
        mockkConstructor(MangaAnnotationRepository::class)
        
        every { anyConstructed<HistoryRepository>().find(any(), any(), any()) } returns listOf()
        every { anyConstructed<MangaAnnotationRepository>().findByManga(any()) } returns listOf()
        
        every { controller.isOnline() } returns true
        every { controller.initialize(any()) } answers {
            firstArg<(ShareMarkType) -> Unit>().invoke(ShareMarkType.SUCCESS)
        }
        ShareMarkBase.IN_SYNC = false
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `mangaShareMark flow with Drive mocks`() = runBlocking {
        val fileList = FileList()
        fileList.files = Collections.emptyList()
        
        every { driveList.setQ(any()) } returns driveList
        every { driveList.setSpaces(any()) } returns driveList
        every { driveList.setFields(any()) } returns driveList
        every { driveList.setOrderBy(any()) } returns driveList
        every { driveList.setPageToken(any()) } returns driveList
        every { driveList.execute() } returns fileList

        val manga = MangaMock.mockEntity()
        every { anyConstructed<MangaRepository>().listSync(any()) } returns listOf(manga)
        every { anyConstructed<MangaRepository>().update(any(), any()) } returns Unit
        every { anyConstructed<MangaRepository>().findByFileName(any()) } returns null
        
        var result: ShareMarkType? = null
        val latch = java.util.concurrent.CountDownLatch(1)
        
        (controller as ShareMark).mangaShareMark(
            update = { },
            ending = { 
                result = it 
                latch.countDown()
            }
        )
        
        latch.await(10, java.util.concurrent.TimeUnit.SECONDS)
        assertEquals(ShareMarkType.SUCCESS, result)
    }
}
