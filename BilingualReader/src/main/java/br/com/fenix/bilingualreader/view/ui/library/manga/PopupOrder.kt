package br.com.fenix.bilingualreader.view.ui.library.manga

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.view.components.TriStateCheckBox
import org.slf4j.LoggerFactory

class PopupOrder : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(PopupOrder::class.java)

    private lateinit var mOrderName: TriStateCheckBox
    private lateinit var mOrderDate: TriStateCheckBox
    private lateinit var mOrderAccess: TriStateCheckBox
    private lateinit var mOrderFavorite: TriStateCheckBox

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.popup_library_ordering, container, false)

        mOrderName = root.findViewById(R.id.popup_library_order_name)
        mOrderDate = root.findViewById(R.id.popup_library_order_date)
        mOrderAccess = root.findViewById(R.id.popup_library_order_access)
        mOrderFavorite = root.findViewById(R.id.popup_library_order_favorite)

        return root
    }


}