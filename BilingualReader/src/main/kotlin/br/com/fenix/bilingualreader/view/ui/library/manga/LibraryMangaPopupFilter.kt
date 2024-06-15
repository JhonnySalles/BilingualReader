package br.com.fenix.bilingualreader.view.ui.library.manga

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


class LibraryMangaPopupFilter : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(LibraryMangaPopupFilter::class.java)

    private lateinit var mViewModel: MangaLibraryViewModel

    private lateinit var mFilterFavorite: CheckBox
    private lateinit var mFilterReading: CheckBox

    private lateinit var mCheckList: ArrayList<CheckBox>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireActivity()).get(MangaLibraryViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_library_filter, container, false)

        mFilterFavorite = root.findViewById(R.id.popup_library_filter_favorite)
        mFilterReading = root.findViewById(R.id.popup_library_filter_reading)

        mCheckList = arrayListOf(mFilterFavorite, mFilterReading)

        setChecked(mCheckList, mViewModel.typeFilter.value ?: Filter.None)
        observer()
        addListener()

        return root
    }

    private fun setChecked(checkboxes: ArrayList<CheckBox>, filter: Filter) {
        for (check in checkboxes)
            check.isChecked = false

        when (filter) {
            Filter.Favorite -> mFilterFavorite.isChecked = true
            Filter.Reading -> mFilterReading.isChecked = true
            else -> {}
        }
    }

    private fun addListener() {
        mFilterFavorite.setOnCheckedChangeListener { _, isChecked ->
            removeListener(mCheckList)
            if (isChecked)
                mViewModel.filterType(Filter.Favorite)
            else
                mViewModel.filterType(Filter.None)
            addListener()
        }

        mFilterReading.setOnCheckedChangeListener { _, isChecked ->
            removeListener(mCheckList)
            if (isChecked)
                mViewModel.filterType(Filter.Reading)
            else
                mViewModel.filterType(Filter.None)
            addListener()
        }
    }

    private fun removeListener(checkboxes : ArrayList<CheckBox>) {
        for (check in checkboxes)
            check.setOnCheckedChangeListener(null)
    }

    private fun observer() {
        mViewModel.typeFilter.observe(viewLifecycleOwner) {
            removeListener(mCheckList)
            setChecked(mCheckList, it)
            addListener()
        }
    }

}