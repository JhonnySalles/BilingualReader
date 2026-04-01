package br.com.fenix.bilingualreader.view.components

import android.content.Context
import android.content.res.Resources
import br.com.fenix.bilingualreader.R
import com.github.mikephil.charting.components.AxisBase
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MonthAxisValueFormatterTest {

    private lateinit var context: Context
    private lateinit var resources: Resources
    private lateinit var formatter: MonthAxisValueFormatter
    private val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    @Before
    fun setup() {
        context = mockk()
        resources = mockk()
        every { context.resources } returns resources
        every { resources.getStringArray(R.array.mouth_descriptions) } returns months
        formatter = MonthAxisValueFormatter(context)
    }

    @Test
    fun `getAxisLabel returns correct month description for valid values`() {
        val axis: AxisBase = mockk()
        
        // Month descriptions mapping: 1f-Jan, 2f-Feb, ..., 12f-Dec
        assertEquals("Jan", formatter.getAxisLabel(1f, axis))
        assertEquals("Jun", formatter.getAxisLabel(6f, axis))
        assertEquals("Dec", formatter.getAxisLabel(12f, axis))
    }

    @Test
    fun `getAxisLabel returns empty string for values out of range`() {
        val axis: AxisBase = mockk()
        
        assertEquals("", formatter.getAxisLabel(0f, axis))
        assertEquals("", formatter.getAxisLabel(13f, axis))
        assertEquals("", formatter.getAxisLabel(-1f, axis))
    }

    @Test
    fun `getAxisLabel handles float values by floor conversion`() {
        val axis: AxisBase = mockk()
        
        // 1.5 - 1 = 0.5 -> 0 -> Jan
        assertEquals("Jan", formatter.getAxisLabel(1.5f, axis))
        // 2.9 - 1 = 1.9 -> 1 -> Feb
        assertEquals("Feb", formatter.getAxisLabel(2.9f, axis))
    }
}
