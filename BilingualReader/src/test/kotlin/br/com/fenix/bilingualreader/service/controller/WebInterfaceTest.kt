package br.com.fenix.bilingualreader.service.controller

import android.app.Activity
import android.content.Context
import br.com.fenix.bilingualreader.view.ui.popup.PopupKanji
import br.com.fenix.bilingualreader.view.ui.popup.PopupVocabulary
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WebInterfaceTest {

    private lateinit var activity: Activity
    private lateinit var context: Context
    private lateinit var webInterface: WebInterface
    
    @Before
    fun setUp() {
        activity = mockk(relaxed = true)
        context = mockk(relaxed = true)
        
        mockkConstructor(PopupKanji::class)
        mockkConstructor(PopupVocabulary::class)
        every { anyConstructed<PopupKanji>().getPopupKanji(any()) } just Runs
        every { anyConstructed<PopupVocabulary>().getPopupVocabulary(any()) } just Runs
        
        // Mock Firebase
        mockkObject(Firebase)
        mockkStatic("com.google.firebase.crashlytics.ktx.FirebaseCrashlyticsKt")
        val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { Firebase.crashlytics } returns crashlytics
        
        // Mock LayoutInflater
        val inflater = mockk<android.view.LayoutInflater>(relaxed = true)
        every { context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) } returns inflater
        every { activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) } returns inflater
        mockkStatic(android.view.LayoutInflater::class)
        every { android.view.LayoutInflater.from(any()) } returns inflater
        
        webInterface = WebInterface(activity, context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `showPopupVocabulary calls activity runOnUiThread and popup getPopupVocabulary`() {
        val slot = slot<Runnable>()
        every { activity.runOnUiThread(capture(slot)) } answers {
            slot.captured.run()
        }
        
        webInterface.showPopupVocabulary(123L)
        
        verify { anyConstructed<PopupVocabulary>().getPopupVocabulary(123L) }
    }

    @Test
    fun `showPopupKanji calls activity runOnUiThread and popup getPopupKanji`() {
        val slot = slot<Runnable>()
        every { activity.runOnUiThread(capture(slot)) } answers {
            slot.captured.run()
        }
        
        webInterface.showPopupKanji("漢")
        
        verify { anyConstructed<PopupKanji>().getPopupKanji("漢") }
    }
}
