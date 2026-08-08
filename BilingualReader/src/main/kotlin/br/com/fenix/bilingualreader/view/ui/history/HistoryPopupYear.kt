package br.com.fenix.bilingualreader.view.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.R

class HistoryPopupYear : Fragment() {

    private lateinit var mViewModel: HistoryViewModel
    private lateinit var mContainer: LinearLayout
    private val mItems = mutableMapOf<Int, CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireParentFragment())[HistoryViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_checkbox_list, container, false)
        mContainer = root.findViewById(R.id.popup_checkbox_list_container)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel.availableYears.observe(viewLifecycleOwner) { buildList(it) }
        mViewModel.selectedYears.observe(viewLifecycleOwner) { updateSelection(it) }
    }

    private fun buildList(years: List<Int>) {
        mContainer.removeAllViews()
        mItems.clear()
        val selected = mViewModel.selectedYears.value.orEmpty()
        val inflater = LayoutInflater.from(requireContext())

        for (year in years) {
            val check = inflater.inflate(R.layout.popup_checkbox_item, mContainer, false) as CheckBox
            check.text = year.toString()
            check.isChecked = year in selected
            check.setOnCheckedChangeListener { _, _ -> applyFilter() }
            mItems[year] = check
            mContainer.addView(check)
        }
    }

    private fun updateSelection(selected: Set<Int>) {
        for ((year, check) in mItems) {
            check.setOnCheckedChangeListener(null)
            check.isChecked = year in selected
            check.setOnCheckedChangeListener { _, _ -> applyFilter() }
        }
    }

    private fun applyFilter() {
        val selected = mItems.filter { it.value.isChecked }.keys.toSet()
        mViewModel.filterYears(selected)
    }
}
