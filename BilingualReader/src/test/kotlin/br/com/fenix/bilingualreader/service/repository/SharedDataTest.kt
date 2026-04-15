package br.com.fenix.bilingualreader.service.repository

import br.com.fenix.bilingualreader.model.entity.Chapters
import br.com.fenix.bilingualreader.service.listener.ChapterLoadListener
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SharedDataTest {

    @Before
    fun setUp() {
        SharedData.clearChapters()
        SharedData.setDocumentParse(null)
    }

    @Test
    fun testChaptersState() {
        val chapters = listOf(
            Chapters("Title", 1, 1, 1f, false),
            Chapters("Title 2", 2, 2, 2f, false)
        )
        val obj = Any()
        
        SharedData.setChapters(obj, chapters)
        
        assertFalse(SharedData.isProcessed(obj))
        assertEquals(2, SharedData.chapters.value?.size)
        
        SharedData.selectPage(2)
        assertTrue(SharedData.chapters.value!![1].isSelected)
        assertFalse(SharedData.chapters.value!![0].isSelected)
    }

    @Test
    fun testListeners() {
        val listener = mockk<ChapterLoadListener>(relaxed = true)
        SharedData.addListener(listener)
        
        SharedData.callListeners(10)
        
        verify { listener.onLoading(10) }
        
        SharedData.remListener(listener)
        SharedData.callListeners(20)
        
        verify(exactly = 0) { listener.onLoading(20) }
    }
}
