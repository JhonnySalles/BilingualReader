package br.com.fenix.bilingualreader.view.components

import android.content.Context
import br.com.fenix.bilingualreader.R
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter


class MonthAxisValueFormatter(context: Context) : ValueFormatter() {

    private val mMonths = context.resources.getStringArray(R.array.mouth_descriptions)

    private fun getDescription(value: Float): String {
        val month = (value - 1).toInt()
        return if (month > -1 && month < mMonths.size) mMonths[month] else ""
    }

    override fun getAxisLabel(value: Float, axis: AxisBase): String {
        return getDescription(value)
    }

}