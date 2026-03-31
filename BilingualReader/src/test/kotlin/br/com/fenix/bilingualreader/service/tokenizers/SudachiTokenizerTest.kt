package br.com.fenix.bilingualreader.service.tokenizers

import android.content.Context
import com.worksap.nlp.sudachi.Dictionary
import com.worksap.nlp.sudachi.Morpheme
import com.worksap.nlp.sudachi.MorphemeList
import com.worksap.nlp.sudachi.Tokenizer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SudachiTokenizerTest {

    private lateinit var context: Context
    private lateinit var mockDictionary: Dictionary
    private lateinit var mockTokenizer: Tokenizer

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockDictionary = mockk()
        mockTokenizer = mockk()
        
        every { mockDictionary.create() } returns mockTokenizer
    }

    @Test
    fun tokenizeString_callsUnderlyingTokenizer() {
        val input = "これはテストです。"
        val mockMorphemeList = mockk<MorphemeList>()
        val mockResult = listOf(mockMorphemeList)
        
        every { mockTokenizer.tokenizeSentences(input) } returns mockResult
        
        val sudachi = SudachiTokenizer(context, mockDictionary)
        val result = sudachi.tokenizeString(input)
        
        assertEquals(mockResult, result)
        verify { mockTokenizer.tokenizeSentences(input) }
    }
}
