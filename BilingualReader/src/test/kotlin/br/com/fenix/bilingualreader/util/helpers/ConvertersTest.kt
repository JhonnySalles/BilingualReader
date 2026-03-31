package br.com.fenix.bilingualreader.util.helpers

import android.graphics.Bitmap
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setUp() {
        converters = Converters()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testBase64Conversions() {
        mockkObject(ImageUtil.ImageUtils)
        val mockBitmap = mockk<Bitmap>(relaxed = true)
        val base64 = "mock_base64"
        
        every { ImageUtil.decodeImageBase64(base64) } returns mockBitmap
        every { ImageUtil.encodeImageBase64(mockBitmap) } returns base64
        
        assertEquals(mockBitmap, converters.fromBase64(base64))
        assertEquals(base64, converters.bitmapToBase64(mockBitmap))
    }

    @Test
    fun testLocalDateTimeConversions() {
        val now = LocalDateTime.now()
        val str = now.toString()
        
        assertEquals(now, converters.fromLocalDateTime(str))
        assertEquals(str, converters.localDateTimeToString(now))
        assertNull(converters.fromLocalDateTime(null))
        assertNull(converters.localDateTimeToString(null))
    }

    @Test
    fun testLocalDateConversions() {
        val today = LocalDate.now()
        val str = today.toString()
        
        assertEquals(today, converters.fromLocalDate(str))
        assertEquals(str, converters.localDateToString(today))
        assertNull(converters.fromLocalDate(null))
        assertNull(converters.localDateToString(null))
    }

    @Test
    fun testIntArrayConversions() {
        val array = intArrayOf(1, 2, 3)
        val str = "1,2,3"
        
        assertArrayEquals(array, converters.fromIntArray(str))
        assertEquals(str, converters.intArrayToString(array))
        
        assertArrayEquals(intArrayOf(), converters.fromIntArray(""))
        assertEquals("", converters.intArrayToString(intArrayOf()))
    }

    @Test
    fun testLongMutableListConversions() {
        val list = mutableListOf(1L, 2L, 3L)
        val str = "1,2,3"
        
        assertEquals(list, converters.fromLongMutableList(str))
        assertEquals(str, converters.longMutableListToString(list))
        
        assertEquals(mutableListOf<Long>(), converters.fromLongMutableList(""))
        assertEquals("", converters.longMutableListToString(mutableListOf()))
    }

    @Test
    fun testIntMapConversions() {
        val map = mapOf(1 to "one", 2 to "two")
        // Gson output for this map
        val str = "{\"1\":\"one\",\"2\":\"two\"}"
        
        assertEquals(map, converters.fromIntMap(str))
        assertEquals(str, converters.intMapToString(map))
        
        assertEquals(mapOf<Int, String>(), converters.fromIntMap(""))
        assertEquals("", converters.intMapToString(mapOf()))
    }

    @Test
    fun testDateConversions() {
        val now = Date()
        val time = now.time
        
        assertEquals(now, converters.fromDate(time))
        assertEquals(time, converters.dateToLong(now))
        assertNull(converters.fromDate(null))
        assertNull(converters.dateToLong(null))
    }
}
