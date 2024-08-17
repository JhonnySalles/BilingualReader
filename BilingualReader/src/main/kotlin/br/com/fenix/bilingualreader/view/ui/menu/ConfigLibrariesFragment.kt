package br.com.fenix.bilingualreader.view.ui.menu

import android.app.Activity.RESULT_OK
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Themes
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.listener.LibrariesCardListener
import br.com.fenix.bilingualreader.service.repository.Storage
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.MenuUtil
import br.com.fenix.bilingualreader.util.helpers.MsgUtil
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.adapter.RecyclerViewItemDecoration
import br.com.fenix.bilingualreader.view.adapter.configuration.LibrariesLineCardAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import org.slf4j.LoggerFactory


class ConfigLibrariesFragment : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(ConfigLibrariesFragment::class.java)

    private val mViewModel: ConfigLibrariesViewModel by viewModels()

    private lateinit var mRecycleView: RecyclerView
    private lateinit var mAddButton: FloatingActionButton
    private lateinit var mListener: LibrariesCardListener
    private lateinit var mToolbar: androidx.appcompat.widget.Toolbar
    private var mType : Type? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAddButton = view.findViewById(R.id.config_library_add_button)
        mAddButton.setOnClickListener { addLibrary() }

        mRecycleView = view.findViewById(R.id.rv_config_library_list)
        mRecycleView.adapter = LibrariesLineCardAdapter()
        val layout = GridLayoutManager(requireContext(), 1)
        mRecycleView.layoutManager = layout
        mRecycleView.layoutAnimation = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_animation_library_line)
        mRecycleView.addItemDecoration(RecyclerViewItemDecoration(requireContext(), R.drawable.config_library_list_divider))

        val theme = Themes.valueOf(GeneralConsts.getSharedPreferences(requireContext()).getString(GeneralConsts.KEYS.THEME.THEME_USED, Themes.ORIGINAL.toString())!!)
        mToolbar = view.findViewById(R.id.toolbar_configuration_libraries)
        MenuUtil.tintToolbar(mToolbar, theme)

        (requireActivity() as MenuActivity).setActionBar(mToolbar)

        mListener = object : LibrariesCardListener {
            override fun onClick(library: Library) {
                addLibrary(library)
            }

            override fun onClickLong(library: Library, view : View, position: Int) {
                val wrapper = ContextThemeWrapper(requireContext(), R.style.PopupMenu)
                val popup = PopupMenu(wrapper, view, 0, R.attr.popupMenuStyle, R.style.PopupMenu)
                popup.menuInflater.inflate(R.menu.menu_libraries_item, popup.menu)

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_libraries_item_delete ->  deleteLibrary(library, position)
                    }
                    true
                }

                popup.show()
            }

            override fun changeEnable(library: Library) {
                mViewModel.saveLibrary(library)
            }
        }
        (mRecycleView.adapter as LibrariesLineCardAdapter).attachListener(mListener)
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecycleView)

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val bundle = Bundle()
                bundle.putBoolean(GeneralConsts.KEYS.LIBRARY.CLEAR_LIBRARY_LIST, mViewModel.mClearLibraries.isNotEmpty())
                if (mType != null)
                    bundle.putString(GeneralConsts.KEYS.LIBRARY.LIBRARY_TYPE, mType.toString())
                bundle.putLongArray(GeneralConsts.KEYS.LIBRARY.LIBRARY_ARRAY_ID, mViewModel.mClearLibraries.toLongArray())
                (requireActivity() as MenuActivity).onBack(bundle)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        observer()

        mType = if(requireArguments().containsKey(GeneralConsts.KEYS.LIBRARY.LIBRARY_TYPE))
            Type.valueOf(requireArguments().getString(GeneralConsts.KEYS.LIBRARY.LIBRARY_TYPE)!!)
        else
            null
        mViewModel.loadLibrary(mType)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_config_libraries, container, false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                requireActivity().onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private var itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val library = mViewModel.getLibraryAndRemove(viewHolder.bindingAdapterPosition) ?: return
            val position = viewHolder.bindingAdapterPosition
            deleteLibrary(library, position)
        }
    }

    private fun notifyDataSet(index: Int, range: Int = 1, insert: Boolean = false, removed: Boolean = false) {
        if (insert)
            mRecycleView.adapter?.notifyItemInserted(index)
        else if (removed)
            mRecycleView.adapter?.notifyItemRemoved(index)
        else if (range > 1)
            mRecycleView.adapter?.notifyItemRangeChanged(index, range)
        else
            mRecycleView.adapter?.notifyItemChanged(index)
    }

    private fun observer() {
        mViewModel.libraries.observe(viewLifecycleOwner) {
            (mRecycleView.adapter as LibrariesLineCardAdapter).updateList(it)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            GeneralConsts.REQUEST.OPEN_MANGA_FOLDER -> {
                if (data != null && resultCode == RESULT_OK) {
                    val folder = Util.normalizeFilePath(data.data?.path.toString())

                    if (!Storage.isPermissionGranted(requireContext()))
                        Storage.takePermission(requireContext(), requireActivity())

                    mLibraryPathAutoComplete.setText(folder)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == GeneralConsts.REQUEST.PERMISSION_FILES_ACCESS) {
            MsgUtil.validPermission(requireContext(), grantResults)
        }
    }


    // --------------------------------------------------------- Popup library ---------------------------------------------------------
    private fun addLibrary(library: Library? = null) {
        val popup = createLibraryPopup(LayoutInflater.from(context), library)

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.AppCompatMaterialAlertDialog)
            .setTitle(getString(R.string.config_libraries_add_library))
            .setView(popup)
            .setCancelable(false)
            .setPositiveButton(
                R.string.action_positive
            ) { _, _ -> }
            .setNegativeButton(
                R.string.action_negative
            ) { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            if (validate()) {
                mViewModel.newLibrary(getLibrary())
                dialog.dismiss()
            }
        }
    }

    private lateinit var mLibraryTitle: TextInputLayout

    private lateinit var mLibraryLanguage: TextInputLayout
    private lateinit var mLibraryLanguageAutoComplete: MaterialAutoCompleteTextView

    private lateinit var mLibraryPath: TextInputLayout
    private lateinit var mLibraryPathAutoComplete: MaterialAutoCompleteTextView

    private var mLibraryTypeSelect: Libraries = Libraries.JAPANESE
    private lateinit var mMapLanguage: HashMap<String, Libraries>

    private fun createLibraryPopup(inflater: LayoutInflater, library: Library?): View? {
        val root = inflater.inflate(R.layout.fragment_config_library, null, false)

        mLibraryTitle = root.findViewById(R.id.libraries_txt_library_title)

        mLibraryLanguage = root.findViewById(R.id.libraries_txt_library_language)
        mLibraryLanguageAutoComplete = root.findViewById(R.id.libraries_menu_autocomplete_library_language)

        mLibraryPath = root.findViewById(R.id.libraries_txt_library_path)
        mLibraryPathAutoComplete = root.findViewById(R.id.libraries_menu_autocomplete_library_path)

        mLibraryPathAutoComplete.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            startActivityForResult(intent, GeneralConsts.REQUEST.OPEN_MANGA_FOLDER)
        }

        val languages = resources.getStringArray(R.array.languages)
        mMapLanguage = hashMapOf(
            languages[1] to Libraries.ENGLISH,
            languages[2] to Libraries.JAPANESE,
            languages[0] to Libraries.PORTUGUESE
        )

        mLibraryLanguageAutoComplete.setAdapter(ArrayAdapter(requireContext(), R.layout.list_item, mMapLanguage.keys.toTypedArray()))
        mLibraryLanguageAutoComplete.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                mLibraryTypeSelect =
                    if (parent.getItemAtPosition(position).toString().isNotEmpty() &&
                        mMapLanguage.containsKey(parent.getItemAtPosition(position).toString())
                    )
                        mMapLanguage[parent.getItemAtPosition(position).toString()]!!
                    else
                        Libraries.JAPANESE
            }

        if (library != null) {
            mLibraryTitle.editText?.setText(library.title)
            mLibraryPathAutoComplete.setText(library.path)
            mLibraryTypeSelect = library.language
        }

        mLibraryLanguageAutoComplete.setText(
            mMapLanguage.entries.first { it.value == mLibraryTypeSelect }.key,
            false
        )

        mLibrary = library

        return root
    }

    private var mLibrary: Library? = null
    private fun getLibrary(): Library {
        if (mLibrary == null)
            mLibrary = Library(null)

        mLibrary?.run {
            title = mLibraryTitle.editText?.text.toString()
            path = mLibraryPath.editText?.text.toString()
            language = mLibraryTypeSelect
            type = mType ?: Type.MANGA
        }

        return mLibrary!!
    }

    fun validate(): Boolean {
        var validated = true

        if (mLibraryTitle.editText?.text?.toString()?.isEmpty() == true) {
            validated = false
            mLibraryTitle.isErrorEnabled = true
            mLibraryTitle.error = getString(R.string.config_libraries_title_library_required)
        }

        if (mLibraryPath.editText?.text?.toString()?.isEmpty() == true) {
            validated = false
            mLibraryPath.isErrorEnabled = true
            mLibraryPath.error = getString(R.string.config_libraries_title_path_required)
        } else {
            val default = mViewModel.getDefault(mType ?: Type.MANGA)

            if (default.isNotEmpty() && mLibraryPath.editText?.text?.toString()?.equals(default, true) == true) {
                validated = false
                mLibraryPath.isErrorEnabled = true
                mLibraryPath.error = getString(R.string.config_libraries_title_path_default)
            }
        }

        return validated
    }

    private fun deleteLibrary(library: Library, position: Int) {
        var excluded = false
        val dialog: AlertDialog =
            MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                .setTitle(getString(R.string.config_libraries_delete_library))
                .setMessage(getString(R.string.config_libraries_confirm_delete_library) + "\n" + library.title)
                .setPositiveButton(
                    R.string.action_delete
                ) { _, _ ->
                    mViewModel.deleteLibrary(library)
                    notifyDataSet(position, removed = true)
                    excluded = true
                }.setOnDismissListener {
                    if (!excluded) {
                        mViewModel.addLibrary(library, position)
                        notifyDataSet(position)
                    }
                }
                .create()
        dialog.show()
    }

}