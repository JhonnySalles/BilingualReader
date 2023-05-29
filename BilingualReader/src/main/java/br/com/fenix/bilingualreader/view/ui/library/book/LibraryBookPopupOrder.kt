package br.com.fenix.bilingualreader.view.ui.library.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Order
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.components.PopupOrderListener
import br.com.fenix.bilingualreader.view.components.TriStateCheckBox
import org.slf4j.LoggerFactory

class LibraryBookPopupOrder(var listener: PopupOrderListener) : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(LibraryBookPopupOrder::class.java)

    private lateinit var mOrderName: TriStateCheckBox
    private lateinit var mOrderDate: TriStateCheckBox
    private lateinit var mOrderAccess: TriStateCheckBox
    private lateinit var mOrderFavorite: TriStateCheckBox
    private lateinit var mOrderGenre: TriStateCheckBox
    private lateinit var mOrderAuthor: TriStateCheckBox

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.popup_library_order_book, container, false)

        mOrderName = root.findViewById(R.id.popup_library_order_book_name)
        mOrderDate = root.findViewById(R.id.popup_library_order_book_date)
        mOrderAccess = root.findViewById(R.id.popup_library_order_book_access)
        mOrderFavorite = root.findViewById(R.id.popup_library_order_book_favorite)
        mOrderGenre = root.findViewById(R.id.popup_library_order_book_genre)
        mOrderAuthor = root.findViewById(R.id.popup_library_order_book_author)

        setChecked(
            getCheckList(),
            listener.popupGetOrder()?.first ?: Order.Name,
            listener.popupGetOrder()?.second ?: false
        )
        addListener()
        observer()
        return root
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
            Order.Genre -> mOrderGenre.state =
                if (isDesc) TriStateCheckBox.STATE_INDETERMINATE else TriStateCheckBox.STATE_CHECKED
            Order.Author -> mOrderAuthor.state =
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

    private fun getCheckList() = arrayListOf(mOrderName, mOrderDate, mOrderAccess, mOrderFavorite, mOrderGenre, mOrderAuthor)

    private fun addListener() {
        val listCheck = getCheckList()

        mOrderName.setOnCheckedChangeListener { _, _ ->
            removeListener(listCheck)

            for (check in listCheck)
                if (check != mOrderName)
                    check.state = TriStateCheckBox.STATE_UNCHECKED

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
            removeListener(listCheck)

            for (check in listCheck)
                if (check != mOrderDate)
                    check.state = TriStateCheckBox.STATE_UNCHECKED

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
            removeListener(listCheck)

            for (check in listCheck)
                if (check != mOrderAccess)
                    check.state = TriStateCheckBox.STATE_UNCHECKED

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
            removeListener(listCheck)

            for (check in listCheck)
                if (check != mOrderFavorite)
                    check.state = TriStateCheckBox.STATE_UNCHECKED

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

        mOrderGenre.setOnCheckedChangeListener { _, _ ->
            removeListener(listCheck)

            for (check in listCheck)
                if (check != mOrderGenre)
                    check.state = TriStateCheckBox.STATE_UNCHECKED

            mOrderGenre.state = getNextState(mOrderGenre)
            when (mOrderGenre.state) {
                TriStateCheckBox.STATE_INDETERMINATE -> listener.popupSorted(Order.Genre, true)
                TriStateCheckBox.STATE_CHECKED -> listener.popupSorted(Order.Genre)
                TriStateCheckBox.STATE_UNCHECKED -> {
                    mOrderName.state = TriStateCheckBox.STATE_CHECKED
                    listener.popupSorted(Order.Name)
                }
                else -> {}
            }

            listener.popupOrderOnChange()
            addListener()
        }

        mOrderAuthor.setOnCheckedChangeListener { _, _ ->
            removeListener(listCheck)

            for (check in listCheck)
                if (check != mOrderAuthor)
                    check.state = TriStateCheckBox.STATE_UNCHECKED

            mOrderAuthor.state = getNextState(mOrderAuthor)
            when (mOrderAuthor.state) {
                TriStateCheckBox.STATE_INDETERMINATE -> listener.popupSorted(Order.Author, true)
                TriStateCheckBox.STATE_CHECKED -> listener.popupSorted(Order.Author)
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
        val listCheck = getCheckList()
        listener.popupGetObserver().observe(viewLifecycleOwner) {
            removeListener(listCheck)
            setChecked(getCheckList(), it.first, it.second)
            addListener()
            save(it.first)
        }
    }

    private fun save(order: Order) {
        val sharedPreferences = GeneralConsts.getSharedPreferences(requireContext())
        with(sharedPreferences.edit()) {
            this!!.putString(GeneralConsts.KEYS.LIBRARY.BOOK_ORDER, order.toString())
            this.commit()
        }
    }

}