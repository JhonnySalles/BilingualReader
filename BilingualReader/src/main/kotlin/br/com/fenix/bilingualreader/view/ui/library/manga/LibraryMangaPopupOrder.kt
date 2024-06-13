package br.com.fenix.bilingualreader.view.ui.library.manga

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.service.listener.PopupOrderListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.components.TriStateCheckBox
import org.slf4j.LoggerFactory


class LibraryMangaPopupOrder : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(LibraryMangaPopupOrder::class.java)

    private lateinit var mOrderName: TriStateCheckBox
    private lateinit var mOrderDate: TriStateCheckBox
    private lateinit var mOrderAccess: TriStateCheckBox
    private lateinit var mOrderFavorite: TriStateCheckBox
    private lateinit var listener: PopupOrderListener

    private lateinit var mCheckList: ArrayList<TriStateCheckBox>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.popup_library_order_manga, container, false)

        mOrderName = root.findViewById(R.id.popup_library_order_manga_name)
        mOrderDate = root.findViewById(R.id.popup_library_order_manga_date)
        mOrderAccess = root.findViewById(R.id.popup_library_order_manga_access)
        mOrderFavorite = root.findViewById(R.id.popup_library_order_manga_favorite)

        mCheckList = arrayListOf(mOrderName, mOrderDate, mOrderAccess, mOrderFavorite)

        var order = Order.Name
        var isDesc = false
        if (::listener.isInitialized) {
            order = listener.popupGetOrder()?.first ?: Order.Name
            isDesc = listener.popupGetOrder()?.second ?: false
        }

        setChecked(mCheckList, order,isDesc)
        addListener()
        observer()
        return root
    }

    fun setListener(listener: PopupOrderListener) {
        this.listener = listener
    }

    private fun setChecked(checkboxes: ArrayList<TriStateCheckBox>, order: Order, isDesc: Boolean) {
        for (check in checkboxes)
            check.state = TriStateCheckBox.STATE_UNCHECKED

        when (order) {
            Order.Name -> mOrderName.state =
                if (isDesc) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
            Order.Date -> mOrderDate.state =
                if (isDesc) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
            Order.LastAccess -> mOrderAccess.state =
                if (isDesc) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
            Order.Favorite -> mOrderFavorite.state =
                if (isDesc) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
            else -> {}
        }
    }

    private fun getNextState(checkbox: TriStateCheckBox): Int {
        return when (checkbox.state) {
            TriStateCheckBox.STATE_UNCHECKED -> TriStateCheckBox.STATE_CHECKED
            TriStateCheckBox.STATE_CHECKED -> TriStateCheckBox.STATE_INDETERMINATE
            TriStateCheckBox.STATE_INDETERMINATE -> TriStateCheckBox.STATE_CHECKED
            else -> TriStateCheckBox.STATE_INDETERMINATE
        }
    }

    private fun addListener() {
        mOrderName.setOnCheckedChangeListener { _, _ ->
            removeListener(mCheckList)

            mOrderName.state = getNextState(mOrderName)
            when (mOrderName.state) {
                TriStateCheckBox.STATE_INDETERMINATE -> listener.popupSorted(Order.Name, true)
                TriStateCheckBox.STATE_CHECKED -> listener.popupSorted(Order.Name)
                TriStateCheckBox.STATE_UNCHECKED -> {
                    mOrderName.state = TriStateCheckBox.STATE_CHECKED
                    listener.popupSorted(Order.Name)
                }
                else -> {}
            }

            listener.popupOrderOnChange()
            addListener()
        }

        mOrderDate.setOnCheckedChangeListener { _, _ ->
            removeListener(mCheckList)

            mOrderDate.state = getNextState(mOrderDate)
            when (mOrderDate.state) {
                TriStateCheckBox.STATE_INDETERMINATE -> listener.popupSorted(Order.Date, true)
                TriStateCheckBox.STATE_CHECKED -> listener.popupSorted(Order.Date)
                TriStateCheckBox.STATE_UNCHECKED -> {
                    mOrderName.state = TriStateCheckBox.STATE_CHECKED
                    listener.popupSorted(Order.Name)
                }
                else -> {}
            }

            listener.popupOrderOnChange()
            addListener()
        }

        mOrderAccess.setOnCheckedChangeListener { _, _ ->
            removeListener(mCheckList)

            mOrderAccess.state = getNextState(mOrderAccess)
            when (mOrderAccess.state) {
                TriStateCheckBox.STATE_INDETERMINATE -> listener.popupSorted(Order.LastAccess, true)
                TriStateCheckBox.STATE_CHECKED -> listener.popupSorted(Order.LastAccess)
                TriStateCheckBox.STATE_UNCHECKED -> {
                    mOrderName.state = TriStateCheckBox.STATE_CHECKED
                    listener.popupSorted(Order.Name)
                }
                else -> {}
            }

            listener.popupOrderOnChange()
            addListener()
        }

        mOrderFavorite.setOnCheckedChangeListener { _, _ ->
            removeListener(mCheckList)

            mOrderFavorite.state = getNextState(mOrderFavorite)
            when (mOrderFavorite.state) {
                TriStateCheckBox.STATE_INDETERMINATE -> listener.popupSorted(Order.Favorite, true)
                TriStateCheckBox.STATE_CHECKED -> listener.popupSorted(Order.Favorite)
                TriStateCheckBox.STATE_UNCHECKED -> {
                    mOrderName.state = TriStateCheckBox.STATE_CHECKED
                    listener.popupSorted(Order.Name)
                }
                else -> {}
            }

            listener.popupOrderOnChange()
            addListener()
        }
    }

    private fun removeListener(checkboxes: ArrayList<TriStateCheckBox>) {
        for (check in checkboxes)
            check.setOnCheckedChangeListener(null)
    }

    private fun observer() {
        listener.popupGetObserver().observe(viewLifecycleOwner) {
            removeListener(mCheckList)
            setChecked(mCheckList, it.first, it.second)
            addListener()
            save(it.first)
        }
    }

    private fun save(order: Order) {
        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        with(sharedPreferences.edit()) {
            this!!.putString(GeneralConsts.KEYS.LIBRARY.MANGA_ORDER, order.toString())
            this.commit()
        }
    }

}