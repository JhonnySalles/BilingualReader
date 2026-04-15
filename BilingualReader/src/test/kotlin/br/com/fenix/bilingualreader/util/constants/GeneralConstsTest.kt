package br.com.fenix.bilingualreader.util.constants

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import br.com.fenix.bilingualreader.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime
import java.util.*

@RunWith(RobolectricTestRunner::class)
class GeneralConstsTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs = context.getSharedPreferences(GeneralConsts.KEYS.PREFERENCE_NAME, Context.MODE_PRIVATE)
        
        // Setup default patterns in prefs
        prefs.edit()
            .putString(GeneralConsts.KEYS.SYSTEM.FORMAT_DATA, "yyyy-MM-dd")
            .putString(GeneralConsts.KEYS.SYSTEM.FORMAT_DATA_SMALL, "yy-MM-dd")
            .apply()
    }

    @Test
    fun `test date time conversion`() {
        val now = LocalDateTime.of(2023, 10, 27, 10, 0)
        val date = GeneralConsts.dateTimeToDate(now)
        val backToNow = GeneralConsts.dateToDateTime(date)
        
        assertEquals(now, backToNow)
    }

    @Test
    fun `test formatCountDays logic`() {
        // Use a fixed today for deterministic testing
        val fixedToday = LocalDateTime.of(2024, 4, 14, 10, 0)
        
        // Mocking the "today" in the test context if possible, but here we just test the logic
        // with dates relative to the logic's definition of "today".
        // Since GeneralConsts.formatCountDays uses LocalDate.now(), we need to be careful.
        // However, we can test relative to current date.
        
        val actualToday = LocalDateTime.now()
        assertEquals(context.getString(R.string.date_format_today), GeneralConsts.formatCountDays(context, actualToday))
        
        val yesterday = actualToday.minusDays(1)
        assertEquals(context.getString(R.string.date_format_yesterday), GeneralConsts.formatCountDays(context, yesterday))
        
        val fourDaysAgo = actualToday.minusDays(4)
        val result = GeneralConsts.formatCountDays(context, fourDaysAgo)
        assertTrue("Expected string to contain '4', but was: $result", result.contains("4"))
        
        val longTimeAgo = actualToday.minusMonths(1)
        val formatted = GeneralConsts.formatCountDays(context, longTimeAgo)
        assertTrue(formatted.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun `test formatterDate with custom pattern`() {
        prefs.edit().putString(GeneralConsts.KEYS.SYSTEM.FORMAT_DATA, "dd/MM/yyyy").apply()
        
        val date = LocalDateTime.of(2025, 4, 14, 0, 0)
        assertEquals("14/04/2025", GeneralConsts.formatterDate(context, date))
    }
}
