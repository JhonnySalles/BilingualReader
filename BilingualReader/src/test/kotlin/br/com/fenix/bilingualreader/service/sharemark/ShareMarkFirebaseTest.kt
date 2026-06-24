package br.com.fenix.bilingualreader.service.sharemark

import android.content.Context
import br.com.fenix.bilingualreader.model.entity.mock.MangaMock
import br.com.fenix.bilingualreader.model.enums.ShareMarkType
import br.com.fenix.bilingualreader.service.repository.MangaRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.*
import io.mockk.anyConstructed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ShareMarkFirebaseTest {

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
        context = mockk<Context>(relaxed = true)
        // Usamos spyk para permitir o mock de métodos protegidos como isOnline()
        controller = spyk(ShareMarkFirebaseController(context), recordPrivateCalls = true)

        mockkStatic(FirebaseFirestore::class)
        mockkStatic(FirebaseAuth::class)
        mockkStatic(GoogleSignIn::class)
        mockkStatic(FirebaseApp::class)
        mockkStatic(GoogleAuthProvider::class)
        mockkStatic(Dispatchers::class)
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")

        every { FirebaseFirestore.getInstance() } returns firestore
        every { FirebaseAuth.getInstance() } returns auth
        every { GoogleSignIn.getLastSignedInAccount(any()) } returns googleSignInAccount
        every { FirebaseApp.initializeApp(any()) } returns mockk()
        every { GoogleAuthProvider.getCredential(any(), any()) } returns mockk()
        every { Dispatchers.IO } returns Dispatchers.Unconfined

        every { googleSignInAccount.email } returns "test@example.com"
        every { googleSignInAccount.idToken } returns "mock_token"

        every { firestore.collection(any()) } returns collection
        every { collection.document(any()) } returns document
        
        // Mock do método protegido isOnline da classe base ShareMarkBase
        every { controller["isOnline"]() } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `mangaShareMark flow`() = runBlocking {
        mockkConstructor(MangaRepository::class)
        val manga = MangaMock.mockEntity()
        every { anyConstructed<MangaRepository>().listSync(any()) } returns listOf(manga)
        
        coEvery { collection.whereGreaterThan(any<String>(), any()).get().await() } returns snapshot
        coEvery { collection.document(any()).get().await() } returns docSnapshot
        every { docSnapshot.exists() } returns false
        every { snapshot.documents } returns listOf()

        var endingResult: ShareMarkType? = null
        (controller as ShareMark).mangaShareMark(
            update = { },
            ending = { endingResult = it }
        )

        assertEquals(ShareMarkType.SUCCESS, endingResult)
        verify { anyConstructed<MangaRepository>().listSync(any()) }
    }
}
