package br.com.fenix.bilingualreader.view.ui.library.manga

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.view.components.TriStateCheckBox
import org.slf4j.LoggerFactory

class PopupFilter : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(PopupFilter::class.java)

    private lateinit var mFilterOrder: TriStateCheckBox
    private lateinit var mFilterReading: TriStateCheckBox

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.popup_library_filter, container, false)

        mFilterOrder = root.findViewById(R.id.popup_library_filter_favorite)
        mFilterReading = root.findViewById(R.id.popup_library_filter_reading)

        return root
    }


}