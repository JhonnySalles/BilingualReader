package br.com.fenix.bilingualreader.view.ui.annotation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Filter
import br.com.fenix.bilingualreader.service.listener.AnnotationListener
import org.slf4j.LoggerFactory


class AnnotationPopupFilterType : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(AnnotationPopupFilterType::class.java)

    private lateinit var mFilterFavorite: CheckBox
    private lateinit var mFilterDetach: CheckBox
    private lateinit var mFilterPageMarker: CheckBox
    private lateinit var mFilterBookMarker: CheckBox

    private lateinit var mCheckMap: Map<CheckBox, Filter>
    private var mListener: AnnotationListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_annotation_filter_type, container, false)

        mFilterFavorite = root.findViewById(R.id.popup_annotation_filter_type_favorite)
        mFilterDetach = root.findViewById(R.id.popup_annotation_filter_type_detach)
        mFilterPageMarker = root.findViewById(R.id.popup_annotation_filter_type_page_marker)
        mFilterBookMarker = root.findViewById(R.id.popup_annotation_filter_type_book_marker)

        mCheckMap = mapOf(mFilterFavorite to Filter.Favorite, mFilterDetach to Filter.Detach, mFilterPageMarker to Filter.PageMark, mFilterBookMarker to Filter.BookMark)

        setChecked(mCheckMap, mListener?.getFilters() ?:setOf())
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
                mListener?.filterType(list[check]!!, !isChecked)
                addListener()
            }

    }

    private fun removeListener(checkboxes : Set<CheckBox>) {
        for (check in checkboxes)
            check.setOnCheckedChangeListener(null)
    }

    fun setListener(listener: AnnotationListener?) {
        mListener = listener
    }

    fun setFilters(filters: Set<Filter>) {
        if (!::mCheckMap.isInitialized)
            return

        removeListener(mCheckMap.keys)
        setChecked(mCheckMap, filters)
        addListener()
    }

}