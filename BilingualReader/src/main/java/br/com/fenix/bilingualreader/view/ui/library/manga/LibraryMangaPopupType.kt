package br.com.fenix.bilingualreader.view.ui.library.manga

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Filter
import br.com.fenix.bilingualreader.model.enums.LibraryMangaType
import br.com.fenix.bilingualreader.view.components.TriStateCheckBox
import org.slf4j.LoggerFactory

class LibraryMangaPopupType : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(LibraryMangaPopupType::class.java)

    private lateinit var mViewModel: MangaLibraryViewModel

    private lateinit var mTypeGridBig: CheckBox
    private lateinit var mTypeGridMedium: CheckBox
    private lateinit var mTypeGridSmall: CheckBox
    private lateinit var mTypeLine: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireActivity()).get(MangaLibraryViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.popup_library_type_manga, container, false)

        mTypeGridBig = root.findViewById(R.id.popup_library_manga_type_grid_big)
        mTypeGridMedium = root.findViewById(R.id.popup_library_manga_type_grid_medium)
        mTypeGridSmall = root.findViewById(R.id.popup_library_manga_type_grid_small)
        mTypeLine = root.findViewById(R.id.popup_library_manga_type_line)

        mTypeGridSmall.visibility = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) View.VISIBLE else View.GONE

        setChecked(getCheckList(), mViewModel.libraryType.value ?: LibraryMangaType.GRID_BIG)
        observer()
        addListener()

        return root
    }

    private fun setChecked(checkboxes: ArrayList<CheckBox>, type: LibraryMangaType) {
        for (check in checkboxes)
            check.isChecked = false

        when (type) {
            LibraryMangaType.GRID_BIG -> mTypeGridBig.isChecked = true
            LibraryMangaType.GRID_MEDIUM -> mTypeGridMedium.isChecked = true
            LibraryMangaType.GRID_SMALL -> mTypeGridSmall.isChecked = true
            LibraryMangaType.LINE -> mTypeLine.isChecked = true
            else -> {}
        }
    }

    private fun getCheckList() = arrayListOf(mTypeGridBig, mTypeGridMedium, mTypeGridSmall, mTypeLine)

    private fun addListener() {
        val list = getCheckList()

        mTypeGridBig.setOnCheckedChangeListener { _, isChecked ->
            removeListener(list)
            if (isChecked)
                mViewModel.setLibraryType(LibraryMangaType.GRID_BIG)
            else
                mViewModel.setLibraryType(LibraryMangaType.LINE)
            addListener()
        }

        mTypeGridMedium.setOnCheckedChangeListener { _, isChecked ->
            removeListener(list)
            if (isChecked)
                mViewModel.setLibraryType(LibraryMangaType.GRID_MEDIUM)
            else
                mViewModel.setLibraryType(LibraryMangaType.GRID_BIG)
            addListener()
        }

        mTypeGridSmall.setOnCheckedChangeListener { _, isChecked ->
            removeListener(list)
            if (isChecked)
                mViewModel.setLibraryType(LibraryMangaType.GRID_SMALL)
            else
                mViewModel.setLibraryType(LibraryMangaType.GRID_BIG)
            addListener()
        }

        mTypeLine.setOnCheckedChangeListener { _, isChecked ->
            removeListener(list)
            if (isChecked)
                mViewModel.setLibraryType(LibraryMangaType.LINE)
            else
                mViewModel.setLibraryType(LibraryMangaType.GRID_BIG)
            addListener()
        }
    }

    private fun removeListener(checkboxes : ArrayList<CheckBox>) {
        for (check in checkboxes)
            check.setOnCheckedChangeListener(null)
    }

    private fun observer() {
        mViewModel.libraryType.observe(viewLifecycleOwner) {
            removeListener(getCheckList())
            setChecked(getCheckList(), it)
            addListener()
        }
    }

}