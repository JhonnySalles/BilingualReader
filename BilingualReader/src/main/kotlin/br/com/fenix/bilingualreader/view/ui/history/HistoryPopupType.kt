package br.com.fenix.bilingualreader.view.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.HistoryType

class HistoryPopupType : Fragment() {

    private lateinit var mViewModel: HistoryViewModel
    private lateinit var mCheckMap: Map<HistoryType, CheckBox>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireParentFragment())[HistoryViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_history_type, container, false)

        mCheckMap = mapOf(
            HistoryType.LINE to root.findViewById(R.id.popup_history_type_line),
            HistoryType.SEPARATOR_BIG to root.findViewById(R.id.popup_history_type_separator_big),
            HistoryType.SEPARATOR_MEDIUM to root.findViewById(R.id.popup_history_type_separator_medium),
            HistoryType.SEPARATOR_CAROUSEL to root.findViewById(R.id.popup_history_type_separator_carousel),
            HistoryType.SEPARATOR_LINE to root.findViewById(R.id.popup_history_type_separator_line)
        )

        setChecked(mViewModel.historyType.value ?: HistoryType.SEPARATOR_LINE)
        observer()
        addListener()
        return root
    }

    private fun setChecked(type: HistoryType) {
        for (check in mCheckMap)
            check.value.isChecked = check.key == type
    }

    private fun addListener() {
        for (check in mCheckMap)
            check.value.setOnCheckedChangeListener { _, isChecked ->
                removeListener()
                if (isChecked)
                    mViewModel.setHistoryType(check.key)
                else if (check.key == HistoryType.LINE)
                    mViewModel.setHistoryType(HistoryType.SEPARATOR_LINE)
                else
                    mViewModel.setHistoryType(HistoryType.LINE)
                addListener()
            }
    }

    private fun removeListener() {
        for (check in mCheckMap)
            check.value.setOnCheckedChangeListener(null)
    }

    private fun observer() {
        mViewModel.historyType.observe(viewLifecycleOwner) {
            removeListener()
            setChecked(it)
            addListener()
        }
    }
}
