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
import br.com.fenix.bilingualreader.model.enums.LibraryMangaType
import org.slf4j.LoggerFactory

class LibraryMangaPopupType : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(LibraryMangaPopupType::class.java)

    private lateinit var mViewModel: MangaLibraryViewModel

    private lateinit var mTypeGridBig: CheckBox
    private lateinit var mTypeGridMedium: CheckBox
    private lateinit var mTypeGridSmall: CheckBox
    private lateinit var mTypeSeparatorMedium: CheckBox
    private lateinit var mTypeSeparatorSmall: CheckBox
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
        mTypeSeparatorMedium = root.findViewById(R.id.popup_library_manga_type_separator_big)
        mTypeSeparatorSmall = root.findViewById(R.id.popup_library_manga_type_separator_medium)
        mTypeLine = root.findViewById(R.id.popup_library_manga_type_line)

        mTypeGridSmall.visibility = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) View.VISIBLE else View.GONE

        setChecked(getCheckList(), mViewModel.libraryType.value ?: LibraryMangaType.GRID_BIG)
        observer()
        addListener(getCheckList())

        return root
    }

    private fun getCheckList() = mapOf(
        Pair(LibraryMangaType.GRID_BIG, mTypeGridBig),
        Pair(LibraryMangaType.GRID_MEDIUM, mTypeGridMedium),
        Pair(LibraryMangaType.GRID_SMALL, mTypeGridSmall),
        Pair(LibraryMangaType.SEPARATOR_BIG, mTypeSeparatorMedium),
        Pair(LibraryMangaType.SEPARATOR_MEDIUM, mTypeSeparatorSmall),
        Pair(LibraryMangaType.LINE, mTypeLine)
    )

    private fun setChecked(checkboxes: Map<LibraryMangaType, CheckBox>, type: LibraryMangaType) {
        for (check in checkboxes)
            check.value.isChecked = check.key == type
    }

    private fun addListener(checkboxes: Map<LibraryMangaType, CheckBox>) {
        val list = getCheckList()

        for (check in checkboxes)
            check.value.setOnCheckedChangeListener { _, isChecked ->
                removeListener(list)
                if (isChecked)
                    mViewModel.setLibraryType(check.key)
                else if (check.key == LibraryMangaType.GRID_BIG)
                    mViewModel.setLibraryType(LibraryMangaType.LINE)
                else
                    mViewModel.setLibraryType(LibraryMangaType.GRID_BIG)
                addListener(list)
            }
    }

    private fun removeListener(checkboxes: Map<LibraryMangaType, CheckBox>) {
        for (check in checkboxes)
            check.value.setOnCheckedChangeListener(null)
    }

    private fun observer() {
        mViewModel.libraryType.observe(viewLifecycleOwner) {
            removeListener(getCheckList())
            setChecked(getCheckList(), it)
            addListener(getCheckList())
        }
    }

}