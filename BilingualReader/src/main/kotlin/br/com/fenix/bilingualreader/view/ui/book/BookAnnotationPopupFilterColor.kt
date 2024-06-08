package br.com.fenix.bilingualreader.view.ui.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.Filter
import org.slf4j.LoggerFactory


class BookAnnotationPopupFilterColor : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(BookAnnotationPopupFilterColor::class.java)

    private lateinit var mViewModel: BookAnnotationViewModel

    private lateinit var mFilterNone: CheckBox
    private lateinit var mFilterYellow: CheckBox
    private lateinit var mFilterRed: CheckBox
    private lateinit var mFilterGreen: CheckBox
    private lateinit var mFilterBlue: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireActivity())[BookAnnotationViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_annotation_filter_color, container, false)

        mFilterNone = root.findViewById(R.id.popup_annotation_filter_color_none)
        mFilterYellow = root.findViewById(R.id.popup_annotation_filter_color_yellow)
        mFilterRed = root.findViewById(R.id.popup_annotation_filter_color_red)
        mFilterGreen = root.findViewById(R.id.popup_annotation_filter_color_green)
        mFilterBlue = root.findViewById(R.id.popup_annotation_filter_color_blue)

        setChecked(getCheckMap(), mViewModel.colorFilter.value ?: setOf())
        observer()
        addListener()

        return root
    }

    private fun setChecked(map: Map<CheckBox, Color>, filter: Set<Color>) {
        for (check in map.keys) {
            val selected = filter.any { it == map[check] }
            if (selected != check.isChecked)
                check.isChecked = selected
        }
    }

    private fun getCheckMap() = mapOf(mFilterNone to Color.None, mFilterYellow to Color.Yellow, mFilterRed to Color.Red, mFilterGreen to Color.Green, mFilterBlue to Color.Blue)

    private fun addListener() {
        val list = getCheckMap()

        for (check in list.keys)
            check.setOnCheckedChangeListener { _, isChecked ->
                removeListener(list.keys)
                mViewModel.filterColor(list[check]!!, !isChecked)
                addListener()
            }

    }

    private fun removeListener(checkboxes : Set<CheckBox>) {
        for (check in checkboxes)
            check.setOnCheckedChangeListener(null)
    }

    private fun observer() {
        mViewModel.colorFilter.observe(viewLifecycleOwner) {
            val list = getCheckMap()
            removeListener(list.keys)
            setChecked(list, it)
            addListener()
        }
    }

}