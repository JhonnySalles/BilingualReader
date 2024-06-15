package br.com.fenix.bilingualreader.view.ui.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Filter
import org.slf4j.LoggerFactory


class BookAnnotationPopupFilterType : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(BookAnnotationPopupFilterType::class.java)

    private lateinit var mViewModel: BookAnnotationViewModel

    private lateinit var mFilterFavorite: CheckBox
    private lateinit var mFilterDetach: CheckBox
    private lateinit var mFilterPageMarker: CheckBox
    private lateinit var mFilterBookMarker: CheckBox

    private lateinit var mCheckMap: Map<CheckBox, Filter>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireActivity())[BookAnnotationViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_annotation_filter_type, container, false)

        mFilterFavorite = root.findViewById(R.id.popup_annotation_filter_type_favorite)
        mFilterDetach = root.findViewById(R.id.popup_annotation_filter_type_detach)
        mFilterPageMarker = root.findViewById(R.id.popup_annotation_filter_type_page_marker)
        mFilterBookMarker = root.findViewById(R.id.popup_annotation_filter_type_book_marker)

        mCheckMap = mapOf(mFilterFavorite to Filter.Favorite, mFilterDetach to Filter.Detach, mFilterPageMarker to Filter.PageMark, mFilterBookMarker to Filter.BookMark)

        setChecked(mCheckMap, mViewModel.typeFilter.value ?: setOf())
        observer()
        addListener()

        return root
    }

    private fun setChecked(map: Map<CheckBox, Filter>, filter: Set<Filter>) {
        for (check in map.keys) {
            val selected = filter.any { it == map[check] }
            if (selected != check.isChecked)
                check.isChecked = selected
        }
    }

    private fun addListener() {
        val list = mCheckMap

        for (check in list.keys)
            check.setOnCheckedChangeListener { _, isChecked ->
                removeListener(list.keys)
                mViewModel.filterType(list[check]!!, !isChecked)
                addListener()
            }

    }

    private fun removeListener(checkboxes : Set<CheckBox>) {
        for (check in checkboxes)
            check.setOnCheckedChangeListener(null)
    }

    private fun observer() {
        mViewModel.typeFilter.observe(viewLifecycleOwner) {
            removeListener(mCheckMap.keys)
            setChecked(mCheckMap, it)
            addListener()
        }
    }

}