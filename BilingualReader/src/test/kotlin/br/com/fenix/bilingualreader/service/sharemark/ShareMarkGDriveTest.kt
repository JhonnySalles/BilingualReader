package br.com.fenix.bilingualreader.service.sharemark

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.mock.MangaMock
import br.com.fenix.bilingualreader.model.enums.ShareMarkType
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.FileList
import io.mockk.*
import io.mockk.anyConstructed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Collections

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ShareMarkGDriveTest {

    private lateinit var context: Context
    private lateinit var controller: ShareMarkGDriveController

    // Google API Mocks
    private val drive = mockk<Drive>(relaxed = true)
    private val driveFiles = mockk<Drive.Files>(relaxed = true)
    private val driveList = mockk<Drive.Files.List>(relaxed = true)
    private val googleSignInAccount = mockk<GoogleSignInAccount>(relaxed = true)
    
    @Before
    fun setup() {
        context = mockk<Context>(relaxed = true)
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
        every { Dispatchers.IO } returns Dispatchers.Unconfined

        mockkConstructor(Drive.Builder::class)
        val builderMock = mockk<Drive.Builder>(relaxed = true)
        every { anyConstructed<Drive.Builder>().setApplicationName(any()) } returns builderMock
        every { builderMock.setApplicationName(any()) } returns builderMock
        every { anyConstructed<Drive.Builder>().build() } returns drive
        every { builderMock.build() } returns drive

        every { drive.files() } returns driveFiles
        every { driveFiles.list() } returns driveList
        
        every { controller["isOnline"]() } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
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

        mockkConstructor(MangaRepository::class)
        val manga = MangaMock.mockEntity()
        every { anyConstructed<MangaRepository>().listSync(any()) } returns listOf(manga)
        
        var result: ShareMarkType? = null
        (controller as ShareMark).mangaShareMark(
            update = { },
            ending = { result = it }
        )
        
        assertEquals(ShareMarkType.SUCCESS, result)
    }
}
