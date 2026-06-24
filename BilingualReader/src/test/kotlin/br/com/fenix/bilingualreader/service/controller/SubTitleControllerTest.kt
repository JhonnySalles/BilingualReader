package br.com.fenix.bilingualreader.service.controller

import android.content.Context
import android.content.res.Resources
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.SubTitleChapter
import br.com.fenix.bilingualreader.model.entity.SubTitlePage
import br.com.fenix.bilingualreader.model.enums.Languages
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SubTitleControllerTest {

    private lateinit var context: Context
    private lateinit var resources: Resources
    
    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        resources = mockk(relaxed = true)
        every { context.resources } returns resources
        every { resources.getString(R.string.popup_reading_manga_subtitle_chapter) } returns "Capitulo"
        every { resources.getString(R.string.popup_reading_manga_subtitle_extra) } returns "Extra"

        // Mock Firebase
        mockkStatic(Firebase::class)
        mockkStatic("com.google.firebase.crashlytics.ktx.FirebaseCrashlyticsKt")
        val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { Firebase.crashlytics } returns crashlytics
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testGetInstance() {
        val instance1 = SubTitleController.getInstance(context)
        val instance2 = SubTitleController.getInstance(context)
        assertNotNull(instance1)
        assertSame(instance1, instance2)
    }

    @Test
    fun testGetChapterKey() {
        val chapter = mockk<SubTitleChapter>()
        every { chapter.language } returns Languages.JAPANESE
        every { chapter.chapter } returns 1.5f
        every { chapter.extra } returns false
        
        val controller = SubTitleController.getInstance(context)
        val key = controller.getChapterKey(chapter)
        
        assertEquals("JAPANESE - Capitulo 1.5", key)
    }

    @Test
    fun testGetPageKey() {
        val page = mockk<SubTitlePage>()
        every { page.number } returns 5
        every { page.name } returns "test_page"
        
        val controller = SubTitleController.getInstance(context)
        val key = controller.getPageKey(page)
        
        assertEquals("005 test_page", key)
    }
}
