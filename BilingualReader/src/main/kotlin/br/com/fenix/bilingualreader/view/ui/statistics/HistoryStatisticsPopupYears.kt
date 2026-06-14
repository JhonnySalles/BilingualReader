package br.com.fenix.bilingualreader.view.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.R

class HistoryStatisticsPopupYears : Fragment() {

    private lateinit var mViewModel: HistoryStatisticsViewModel
    private lateinit var mContainer: LinearLayout
    private val mCheckBoxes = mutableListOf<Pair<Int?, CheckBox>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireParentFragment())[HistoryStatisticsViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_checkbox_list, container, false)
        mContainer = root.findViewById(R.id.popup_checkbox_list_container)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.years.observe(viewLifecycleOwner) {
            buildList()
        }
        mViewModel.selectedYears.observe(viewLifecycleOwner) { selected ->
            updateSelection(selected)
        }
    }

    private fun buildList() {
        mContainer.removeAllViews()
        mCheckBoxes.clear()

        val inflater = LayoutInflater.from(context)

        // 1. "Todos"
        val cbAll = inflater.inflate(R.layout.popup_checkbox_item, mContainer, false) as CheckBox
        cbAll.text = getString(R.string.history_menu_choice_all)
        mCheckBoxes.add(Pair(null, cbAll))
        mContainer.addView(cbAll)

        // 2. Active Years
        val yearsList = (mViewModel.years.value ?: emptyList()).sortedDescending()
        for (year in yearsList) {
            val cb = inflater.inflate(R.layout.popup_checkbox_item, mContainer, false) as CheckBox
            cb.text = year.toString()
            mCheckBoxes.add(Pair(year, cb))
            mContainer.addView(cb)
        }

        setupListeners()
        updateSelection(mViewModel.selectedYears.value ?: emptySet())
    }

    private fun setupListeners() {
        for (item in mCheckBoxes) {
            val year = item.first
            val cb = item.second
            cb.setOnCheckedChangeListener { _, isChecked ->
                removeListeners()
                if (year == null) {
                    if (isChecked) {
                        mViewModel.filterYear(null)
                    } else {
                        cb.isChecked = true
                    }
                } else {
                    mViewModel.filterYear(year)
                }
                setupListeners()
            }
        }
    }

    private fun removeListeners() {
        for (item in mCheckBoxes) {
            item.second.setOnCheckedChangeListener(null)
        }
    }

    private fun updateSelection(selected: Set<Int>) {
        removeListeners()
        for (item in mCheckBoxes) {
            val year = item.first
            val cb = item.second
            if (year == null) {
                cb.isChecked = selected.isEmpty()
            } else {
                cb.isChecked = selected.contains(year)
            }
        }
        setupListeners()
    }
}
