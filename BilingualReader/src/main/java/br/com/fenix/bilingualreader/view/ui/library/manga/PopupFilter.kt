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
import br.com.fenix.bilingualreader.view.components.TriStateCheckBox
import org.slf4j.LoggerFactory

class PopupFilter : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(PopupFilter::class.java)

    private lateinit var mViewModel: MangaLibraryViewModel

    private lateinit var mFilterFavorite: CheckBox
    private lateinit var mFilterReading: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireActivity()).get(MangaLibraryViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.popup_library_filter, container, false)

        mFilterFavorite = root.findViewById(R.id.popup_library_filter_favorite)
        mFilterReading = root.findViewById(R.id.popup_library_filter_reading)

        mFilterFavorite.isChecked = false
        mFilterReading.isChecked = false

        when (mViewModel.getFilterType()) {
            Filter.Favorite -> mFilterFavorite.isChecked = true
            Filter.Reading -> mFilterReading.isChecked = true
            else -> {}
        }

        addListener()

        return root
    }


    private fun addListener() {
        val list = arrayListOf(mFilterFavorite, mFilterReading)

        mFilterFavorite.setOnCheckedChangeListener { _, isChecked ->
            removeListener(list)
            if (isChecked) {
                mFilterReading.isChecked = false
                mViewModel.filterType(Filter.Favorite)
            } else
                mViewModel.filterType(Filter.None)

            addListener()
        }

        mFilterReading.setOnCheckedChangeListener { _, isChecked ->
            removeListener(list)
            mViewModel.filterType(Filter.Reading)
            if (isChecked) {
                mFilterFavorite.isChecked = false
                mViewModel.filterType(Filter.Reading)
            } else
                mViewModel.filterType(Filter.None)

            addListener()
        }
    }

    private fun removeListener(checkboxes : ArrayList<CheckBox>) {
        for (check in checkboxes)
            check.setOnCheckedChangeListener(null)
    }

}