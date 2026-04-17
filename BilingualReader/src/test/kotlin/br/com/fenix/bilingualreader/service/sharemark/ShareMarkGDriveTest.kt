package br.com.fenix.bilingualreader.service.sharemark

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.model.entity.mock.MangaMock
import br.com.fenix.bilingualreader.model.enums.ShareMarkType
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.repository.HistoryRepository
import br.com.fenix.bilingualreader.service.repository.MangaAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.FileList
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDateTime
import java.util.Collections
import java.util.Date
import com.google.api.services.drive.model.File as DriveFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ShareMarkGDriveTest {

    @Rule @JvmField
    val tempFolder = TemporaryFolder()

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
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
        mockkStatic(FirebaseCrashlytics::class)
        val mockCrashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns mockCrashlytics
        
        mockkStatic(FirebaseApp::class)
        val mockFirebaseApp = mockk<FirebaseApp>(relaxed = true)
        every { FirebaseApp.initializeApp(any<Context>()) } returns mockFirebaseApp
        every { FirebaseApp.getInstance() } returns mockFirebaseApp
        every { mockFirebaseApp.get(FirebaseCrashlytics::class.java) } returns mockCrashlytics

        context = ApplicationProvider.getApplicationContext()
        
        mockkStatic(GoogleSignIn::class)
        mockkStatic(AndroidHttp::class)
        mockkStatic(JacksonFactory::class)
        mockkStatic(GoogleAccountCredential::class)
        mockkStatic(Dispatchers::class)

        every { GoogleSignIn.getLastSignedInAccount(any<Context>()) } returns googleSignInAccount
        every { AndroidHttp.newCompatibleTransport() } returns mockk()
        every { JacksonFactory.getDefaultInstance() } returns mockk()
        every { GoogleAccountCredential.usingOAuth2(any<Context>(), any()) } returns mockk(relaxed = true)
        every { Dispatchers.IO } returns testDispatcher

        controller = spyk(ShareMarkGDriveController(context), recordPrivateCalls = true)

        mockkConstructor(Drive.Builder::class)
        val builderMock = mockk<Drive.Builder>(relaxed = true)
        every { anyConstructed<Drive.Builder>().setApplicationName(any<String>()) } returns builderMock
        every { builderMock.setApplicationName(any<String>()) } returns builderMock
        every { anyConstructed<Drive.Builder>().build() } returns drive
        every { builderMock.build() } returns drive

        every { drive.files() } returns driveFiles
        every { driveFiles.list() } returns driveList
        
        val driveCreate = mockk<Drive.Files.Create>(relaxed = true)
        val driveUpdate = mockk<Drive.Files.Update>(relaxed = true)
        val driveFile = DriveFile().setId("mock_id")
        
        every { driveFiles.create(any<DriveFile>()) } returns driveCreate
        every { driveFiles.create(any<DriveFile>(), any<AbstractInputStreamContent>()) } returns driveCreate
        every { driveCreate.setFields(any<String>()) } returns driveCreate
        every { driveCreate.execute() } returns driveFile
        
        every { driveFiles.update(any<String>(), any<DriveFile>()) } returns driveUpdate
        every { driveFiles.update(any<String>(), any<DriveFile>(), any<AbstractInputStreamContent>()) } returns driveUpdate
        every { driveUpdate.execute() } returns driveFile
        
        every { driveFiles.get(any<String>()) } returns mockk(relaxed = true)
        
        mockkObject(GeneralConsts.Companion)
        val testCacheDir = tempFolder.newFolder("test_cache")
        every { GeneralConsts.getCacheDir(any<Context>()) } returns testCacheDir
        
        // Mock SharedPreferences
        val prefs = mockk<android.content.SharedPreferences>(relaxed = true)
        every { GeneralConsts.getSharedPreferences(any<Context>()) } returns prefs
        every { prefs.getString(any<String>(), any()) } returns "2000-01-01T01:01:01.001-0300"
        
        mockkConstructor(MangaRepository::class)
        mockkConstructor(HistoryRepository::class)
        mockkConstructor(MangaAnnotationRepository::class)
        
        every { anyConstructed<HistoryRepository>().find(any<Type>(), any<Long>(), any<Long>()) } returns listOf()
        every { anyConstructed<MangaAnnotationRepository>().findByManga(any<Long>()) } returns listOf()
        
        every { controller.isOnline() } returns true
        every { controller.initialize(any<(ShareMarkType) -> Unit>()) } answers {
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
        
        every { driveList.setQ(any<String>()) } returns driveList
        every { driveList.setSpaces(any<String>()) } returns driveList
        every { driveList.setFields(any<String>()) } returns driveList
        every { driveList.setOrderBy(any<String>()) } returns driveList
        every { driveList.setPageToken(any<String>()) } returns driveList
        every { driveList.execute() } returns fileList

        val manga = MangaMock.mockEntity()
        every { anyConstructed<MangaRepository>().listSync(any<Date>()) } returns listOf(manga)
        every { anyConstructed<MangaRepository>().update(any<br.com.fenix.bilingualreader.model.entity.Manga>(), any<LocalDateTime>()) } returns Unit
        every { anyConstructed<MangaRepository>().findByFileName(any<String>()) } returns null
        
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
