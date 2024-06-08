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


class BookAnnotationPopupFilter : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(BookAnnotationPopupFilter::class.java)

    private lateinit var mViewModel: BookAnnotationViewModel

    private lateinit var mFilterFavorite: CheckBox
    private lateinit var mFilterDetach: CheckBox
    private lateinit var mFilterPageMarker: CheckBox
    private lateinit var mFilterBookMarker: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireActivity())[BookAnnotationViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_annotation_filter, container, false)

        mFilterFavorite = root.findViewById(R.id.popup_annotation_filter_favorite)
        mFilterDetach = root.findViewById(R.id.popup_annotation_filter_detach)
        mFilterPageMarker = root.findViewById(R.id.popup_annotation_filter_page_marker)
        mFilterBookMarker = root.findViewById(R.id.popup_annotation_filter_book_marker)

        setChecked(getCheckMap(), mViewModel.typeFilter.value ?: setOf())
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

    private fun getCheckMap() = mapOf(mFilterFavorite to Filter.Favorite, mFilterDetach to Filter.Detach, mFilterPageMarker to Filter.PageMark, mFilterBookMarker to Filter.BookMark)

    private fun addListener() {
        val list = getCheckMap()

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
            val list = getCheckMap()
            removeListener(list.keys)
            setChecked(list, it)
            addListener()
        }
    }

}