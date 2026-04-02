package br.com.fenix.bilingualreader.service.sharemark

import br.com.fenix.bilingualreader.util.constants.GeneralConsts

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.mock.MangaMock
import br.com.fenix.bilingualreader.model.enums.ShareMarkType
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import br.com.fenix.bilingualreader.service.repository.HistoryRepository
import br.com.fenix.bilingualreader.service.repository.MangaAnnotationRepository
import java.util.Date
import com.google.firebase.crashlytics.ktx.crashlytics
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ShareMarkFirebaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var controller: ShareMarkFirebaseController

    private val firestore = mockk<FirebaseFirestore>(relaxed = true)
    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val googleSignInAccount = mockk<GoogleSignInAccount>(relaxed = true)
    private val collection = mockk<CollectionReference>(relaxed = true)
    private val document = mockk<DocumentReference>(relaxed = true)
    private val snapshot = mockk<QuerySnapshot>(relaxed = true)
    private val docSnapshot = mockk<DocumentSnapshot>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk<Context>(relaxed = true)
        
        controller = spyk(ShareMarkFirebaseController(context))

        mockkStatic(FirebaseFirestore::class)
        mockkStatic(FirebaseAuth::class)
        mockkStatic(GoogleSignIn::class)
        mockkStatic(FirebaseApp::class)
        mockkStatic(GoogleAuthProvider::class)
        mockkStatic(Dispatchers::class)
        mockkObject(Firebase)
        mockkStatic("com.google.firebase.crashlytics.ktx.FirebaseCrashlyticsKt")
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")

        mockkConstructor(MangaRepository::class)
        mockkConstructor(HistoryRepository::class)
        mockkConstructor(MangaAnnotationRepository::class)

        every { FirebaseFirestore.getInstance() } returns firestore
        every { FirebaseAuth.getInstance() } returns auth
        every { GoogleSignIn.getLastSignedInAccount(context) } returns googleSignInAccount
        every { FirebaseApp.initializeApp(any()) } returns mockk()
        every { FirebaseApp.getInstance() } returns mockk(relaxed = true)
        every { Firebase.crashlytics } returns mockk(relaxed = true)
        every { GoogleAuthProvider.getCredential(any(), any()) } returns mockk()
        every { Dispatchers.IO } returns testDispatcher

        every { googleSignInAccount.email } returns "test@example.com"
        every { googleSignInAccount.idToken } returns "mock_token"

        every { firestore.collection(any()) } returns collection
        every { collection.document(any()) } returns document
        
        val query = mockk<com.google.firebase.firestore.Query>(relaxed = true)
        every { collection.whereGreaterThan(any<String>(), any()) } returns query
        
        val queryTask = mockk<Task<QuerySnapshot>>(relaxed = true)
        every { query.get() } returns queryTask
        coEvery { queryTask.await() } returns snapshot
        
        val docTask = mockk<Task<DocumentSnapshot>>(relaxed = true)
        every { document.get() } returns docTask
        coEvery { docTask.await() } returns docSnapshot
        
        every { controller.initialize(any()) } answers {
            firstArg<(ShareMarkType) -> Unit>().invoke(ShareMarkType.SUCCESS)
        }
        ShareMarkBase.IN_SYNC = false
        every { Dispatchers.IO } returns testDispatcher
        
        
        // Manual initialization of late-init fields to avoid the crash
        try {
            val mDBField: Field = ShareMarkFirebaseController::class.java.getDeclaredField("mDB")
            mDBField.isAccessible = true
            mDBField.set(controller, firestore)
            
            val mUserField: Field = ShareMarkFirebaseController::class.java.getDeclaredField("mUser")
            mUserField.isAccessible = true
            mUserField.set(controller, "test_user")
        } catch (e: Exception) {}

        // Mock SharedPreferences
        val prefs = mockk<android.content.SharedPreferences>(relaxed = true)
        mockkObject(GeneralConsts.Companion)
        every { GeneralConsts.getSharedPreferences(context) } returns prefs
        every { prefs.getString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_MANGA, any()) } returns ShareMarkBase.INITIAL_SYNC_DATE_TIME
        every { prefs.getString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_BOOK, any()) } returns ShareMarkBase.INITIAL_SYNC_DATE_TIME

        // Mock repository methods
        every { anyConstructed<MangaRepository>().listSync(any()) } returns listOf()
        every { anyConstructed<MangaRepository>().findByFileName(any()) } returns null
        every { anyConstructed<MangaRepository>().update(any(), any()) } returns Unit
        
        every { anyConstructed<HistoryRepository>().find(any(), any(), any()) } returns listOf()
        every { anyConstructed<MangaAnnotationRepository>().findByManga(any()) } returns listOf()
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `mangaShareMark flow`() = runBlocking {
        val manga = MangaMock.mockEntity()
        every { anyConstructed<MangaRepository>().listSync(any()) } returns listOf(manga)
        every { anyConstructed<MangaRepository>().update(any(), any()) } returns Unit
        
        every { docSnapshot.exists() } returns false
        every { snapshot.documents } returns listOf()

        var endingResult: ShareMarkType? = null
        val latch = java.util.concurrent.CountDownLatch(1)
        
        (controller as ShareMark).mangaShareMark(
            update = { },
            ending = { 
                endingResult = it 
                latch.countDown()
            }
        )

        latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
        assertEquals(ShareMarkType.SUCCESS, endingResult)
        verify { anyConstructed<MangaRepository>().listSync(any()) }
    }
}
