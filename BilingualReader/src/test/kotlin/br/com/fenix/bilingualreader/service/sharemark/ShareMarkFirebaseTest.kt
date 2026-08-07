package br.com.fenix.bilingualreader.service.sharemark

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import br.com.fenix.bilingualreader.model.entity.mock.MangaMock
import br.com.fenix.bilingualreader.model.enums.ShareMarkType
import br.com.fenix.bilingualreader.service.repository.HistoryRepository
import br.com.fenix.bilingualreader.service.repository.MangaAnnotationRepository
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.ktx.Firebase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
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

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class ShareMarkFirebaseTest {

    @Rule @JvmField
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var controller: ShareMarkFirebaseController

    private val firestore = mockk<FirebaseFirestore>(relaxed = true)
    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val firebaseUser = mockk<FirebaseUser>(relaxed = true)
    private val collection = mockk<CollectionReference>(relaxed = true)
    private val document = mockk<DocumentReference>(relaxed = true)
    private val snapshot = mockk<QuerySnapshot>(relaxed = true)
    private val docSnapshot = mockk<DocumentSnapshot>(relaxed = true)

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
        
        controller = spyk(ShareMarkFirebaseController(context))

        mockkStatic(FirebaseFirestore::class)
        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseApp::class)
        mockkStatic(Dispatchers::class)
        mockkObject(Firebase)
        mockkStatic("com.google.firebase.crashlytics.ktx.FirebaseCrashlyticsKt")
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")

        mockkConstructor(MangaRepository::class)
        mockkConstructor(HistoryRepository::class)
        mockkConstructor(MangaAnnotationRepository::class)

        every { FirebaseFirestore.getInstance() } returns firestore
        every { FirebaseAuth.getInstance() } returns auth
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.email } returns "test@example.com"
        every { FirebaseApp.initializeApp(any()) } returns mockk()
        every { FirebaseApp.getInstance() } returns mockk(relaxed = true)
        every { Firebase.crashlytics } returns mockk(relaxed = true)
        every { Dispatchers.IO } returns testDispatcher

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
        
        val voidTask = mockk<Task<Void>>(relaxed = true)
        every { document.set(any()) } returns voidTask
        coEvery { voidTask.await() } returns mockk()
        
        every { controller.initialize(any()) } answers {
            firstArg<(ShareMarkType) -> Unit>().invoke(ShareMarkType.SUCCESS)
        }
        
        mockkObject(GeneralConsts.Companion)
        val testCacheDir = tempFolder.newFolder("test_cache")
        every { GeneralConsts.getCacheDir(any()) } returns testCacheDir
        every { controller.isOnline() } returns true
        ShareMarkBase.IN_SYNC = false
        every { Dispatchers.IO } returns testDispatcher
        
        // Manual initialization of late-init fields to avoid the crash
        try {
            val mDBField = ShareMarkFirebaseController::class.java.getDeclaredField("mDB")
            mDBField.isAccessible = true
            mDBField.set(controller, firestore)
            
            val mUserField = ShareMarkFirebaseController::class.java.getDeclaredField("mUser")
            mUserField.isAccessible = true
            mUserField.set(controller, "test_user")
        } catch (e: Exception) {}

        // Mock SharedPreferences and Cache
        val prefs = mockk<android.content.SharedPreferences>(relaxed = true)
        every { GeneralConsts.getSharedPreferences(any()) } returns prefs
        every { prefs.getString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_MANGA, any()) } returns ShareMarkBase.INITIAL_SYNC_DATE_TIME
        every { prefs.getString(GeneralConsts.KEYS.SHARE_MARKS.LAST_SYNC_BOOK, any()) } returns ShareMarkBase.INITIAL_SYNC_DATE_TIME

        // Mock repository methods
        val manga = MangaMock.mockEntity()
        every { anyConstructed<MangaRepository>().listSync(any()) } returns listOf(manga)
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

        latch.await(10, java.util.concurrent.TimeUnit.SECONDS)
        assertEquals(ShareMarkType.SUCCESS, endingResult)
        verify { anyConstructed<MangaRepository>().listSync(any()) }
    }
}
