package br.com.fenix.bilingualreader.util.helpers

import android.graphics.Bitmap
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ConvertersTest {

    private val converters = Converters()

    @Before
    fun setup() {
        mockkObject(ImageUtil.ImageUtils)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `test bitmap converters`() {
        val mockBitmap = mockk<Bitmap>()
        val base64 = "base64string"
        
        every { ImageUtil.encodeImageBase64(mockBitmap) } returns base64
        every { ImageUtil.decodeImageBase64(base64) } returns mockBitmap
        
        assertEquals(base64, converters.bitmapToBase64(mockBitmap))
        assertEquals(mockBitmap, converters.fromBase64(base64))
    }

    @Test
    fun `test LocalDateTime converters`() {
        val now = LocalDateTime.now()
        val str = now.toString()
        
        assertEquals(str, converters.localDateTimeToString(now))
        assertEquals(now, converters.fromLocalDateTime(str))
        assertNull(converters.localDateTimeToString(null))
        assertNull(converters.fromLocalDateTime(null))
    }

    @Test
    fun `test LocalDate converters`() {
        val now = LocalDate.now()
        val str = now.toString()
        
        assertEquals(str, converters.localDateToString(now))
        assertEquals(now, converters.fromLocalDate(str))
    }

    @Test
    fun `test array and list converters`() {
        val intArray = intArrayOf(1, 2, 3)
        val intStr = "1,2,3"
        assertEquals(intStr, converters.intArrayToString(intArray))
        assert(intArray.contentEquals(converters.fromIntArray(intStr)))
        
        val longList = mutableListOf(1L, 2L, 3L)
        val longStr = "1,2,3"
        assertEquals(longStr, converters.longMutableListToString(longList))
        assertEquals(longList, converters.fromLongMutableList(longStr))
    }

    @Test
    fun `test map converters`() {
        val map = mapOf(1 to "one", 2 to "two")
        val json = converters.intMapToString(map)
        val back = converters.fromIntMap(json)
        
        assertEquals(map, back)
        assertEquals("", converters.intMapToString(mapOf()))
        assertEquals(0, converters.fromIntMap("").size)
    }
}
