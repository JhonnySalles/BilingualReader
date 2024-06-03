package br.com.fenix.bilingualreader.view.ui.library.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.LibraryBookType
import org.slf4j.LoggerFactory

class LibraryBookPopupType : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(LibraryBookPopupType::class.java)

    private lateinit var mViewModel: BookLibraryViewModel

    private lateinit var mTypeGrid: CheckBox
    private lateinit var mTypeLine: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel = ViewModelProvider(requireActivity()).get(BookLibraryViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.popup_library_type_book, container, false)

        mTypeGrid = root.findViewById(R.id.popup_library_book_type_grid)
        mTypeLine = root.findViewById(R.id.popup_library_book_type_line)

        setChecked(getCheckList(), mViewModel.libraryType.value ?: LibraryBookType.GRID)
        observer()
        addListener()

        return root
    }

    private fun setChecked(checkboxes: ArrayList<CheckBox>, type: LibraryBookType) {
        for (check in checkboxes)
            check.isChecked = false

        when (type) {
            LibraryBookType.GRID -> mTypeGrid.isChecked = true
            LibraryBookType.LINE -> mTypeLine.isChecked = true
            else -> {}
        }
    }

    private fun getCheckList() = arrayListOf(mTypeGrid, mTypeLine)

    private fun addListener() {
        val list = getCheckList()

        mTypeGrid.setOnCheckedChangeListener { _, isChecked ->
            removeListener(list)
            if (isChecked) {
                mTypeLine.isChecked = false
                mViewModel.setLibraryType(LibraryBookType.GRID)
            } else {
                mTypeLine.isChecked = true
                mViewModel.setLibraryType(LibraryBookType.LINE)
            }

            addListener()
        }

        mTypeLine.setOnCheckedChangeListener { _, isChecked ->
            removeListener(list)
            if (isChecked)
                mViewModel.setLibraryType(LibraryBookType.LINE)
            else
                mViewModel.setLibraryType(LibraryBookType.GRID)

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