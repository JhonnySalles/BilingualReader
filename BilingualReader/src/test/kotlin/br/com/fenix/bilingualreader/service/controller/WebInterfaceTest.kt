package br.com.fenix.bilingualreader.service.controller

import android.app.Activity
import android.content.Context
import br.com.fenix.bilingualreader.view.ui.popup.PopupKanji
import br.com.fenix.bilingualreader.view.ui.popup.PopupVocabulary
import io.mockk.*
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
        
        webInterface = WebInterface(activity, context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testShowPopupVocabulary() {
        val id = 123L
        val slot = slot<Runnable>()
        every { activity.runOnUiThread(capture(slot)) } answers {
            slot.captured.run()
        }
        
        webInterface.showPopupVocabulary(id)
        
        verify { anyConstructed<PopupVocabulary>().getPopupVocabulary(id) }
    }

    @Test
    fun testShowPopupKanji() {
        val kanji = "漢"
        val slot = slot<Runnable>()
        every { activity.runOnUiThread(capture(slot)) } answers {
            slot.captured.run()
        }
        
        webInterface.showPopupKanji(kanji)
        
        verify { anyConstructed<PopupKanji>().getPopupKanji(kanji) }
    }
}
