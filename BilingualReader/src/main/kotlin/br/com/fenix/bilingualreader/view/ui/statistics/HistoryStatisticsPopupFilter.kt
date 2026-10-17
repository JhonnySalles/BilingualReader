package br.com.fenix.bilingualreader.view.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.R

class HistoryStatisticsPopupFilter : Fragment() {

    private lateinit var mViewModel: HistoryStatisticsViewModel
    private lateinit var mCheckGrouped: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireParentFragment())[HistoryStatisticsViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_history_filter, container, false)

        mCheckGrouped = root.findViewById(R.id.popup_history_filter_grouped)

        setChecked(mViewModel.groupedFilter.value ?: false)
        observer()
        addListener()
        return root
    }

    private fun setChecked(grouped: Boolean) {
        mCheckGrouped.isChecked = grouped
    }

    private fun addListener() {
        mCheckGrouped.setOnCheckedChangeListener { _, isChecked ->
            removeListener()
            mViewModel.setGrouped(isChecked)
            addListener()
        }
    }

    private fun removeListener() {
        mCheckGrouped.setOnCheckedChangeListener(null)
    }

    private fun observer() {
        mViewModel.groupedFilter.observe(viewLifecycleOwner) {
            removeListener()
            setChecked(it)
            addListener()
        }
    }
}
