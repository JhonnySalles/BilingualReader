package br.com.fenix.bilingualreader.view.ui.history

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.listener.MangaCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import br.com.fenix.bilingualreader.util.helpers.LibraryUtil
import br.com.fenix.bilingualreader.view.adapter.history.HistoryCardAdapter
import br.com.fenix.bilingualreader.view.ui.reader.manga.MangaReaderActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.time.LocalDateTime


class HistoryFragment : Fragment() {

    private lateinit var mViewModel: HistoryViewModel
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mListener: MangaCardListener
    private lateinit var miSearch: MenuItem
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_history_manga, menu)
        super.onCreateOptionsMenu(menu, inflater)

        val miLibrary = menu.findItem(R.id.menu_history_manga_library)
        miLibrary.subMenu?.clear()
        miLibrary.subMenu?.add(requireContext().getString(R.string.history_menu_choice_library_all))
            ?.setOnMenuItemClickListener { _: MenuItem? ->
                filterLibrary(null)
                true
            }
        for (library in mViewModel.getLibraryList())
            miLibrary.subMenu?.add(library.title)?.setOnMenuItemClickListener { _: MenuItem? ->
                filterLibrary(library)
                true
            }

        miSearch = menu.findItem(R.id.menu_history_manga_search)
        searchView = miSearch.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filter(newText)
                return false
            }
        })
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_history_manga_library -> {}
        }
        return super.onOptionsItemSelected(menuItem)
    }

    private fun filter(text: String?) {
        mViewModel.filter.filter(text)
    }

    private fun filterLibrary(library: Library?) {
        mViewModel.filterLibrary(library)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mViewModel = ViewModelProvider(this).get(HistoryViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_manga_history, container, false)
        mRecyclerView = root.findViewById(R.id.history_list)
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecyclerView)
        mListener = object : MangaCardListener {
            override fun onClick(manga: Manga) {
                if (!manga.excluded && manga.file.exists()) {
                    val intent = Intent(context, MangaReaderActivity::class.java)
                    val bundle = Bundle()
                    manga.lastAccess = LocalDateTime.now()
                    bundle.putSerializable(
                        GeneralConsts.KEYS.OBJECT.LIBRARY,
                        LibraryUtil.getDefault(requireContext(), Type.MANGA)
                    )
                    bundle.putString(GeneralConsts.KEYS.MANGA.NAME, manga.title)
                    bundle.putInt(GeneralConsts.KEYS.MANGA.MARK, manga.bookMark)
                    bundle.putSerializable(GeneralConsts.KEYS.OBJECT.MANGA, manga)
                    intent.putExtras(bundle)
                    context?.startActivity(intent)
                    mViewModel.updateLastAccess(manga)
                } else {
                    if (!manga.excluded) {
                        manga.excluded = true
                        mViewModel.updateDelete(manga)
                        mRecyclerView.adapter?.let {
                            (it as HistoryCardAdapter).notifyItemChanged(manga)
                        }
                    }

                    MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                        .setTitle(getString(R.string.manga_excluded))
                        .setMessage(manga.file.path)
                        .setPositiveButton(
                            R.string.action_neutral
                        ) { _, _ -> }
                        .create()
                        .show()
                }
            }

            override fun onClickLong(manga: Manga, view: View, position: Int) {
                val wrapper = ContextThemeWrapper(requireContext(), R.style.PopupMenu)
                val popup = PopupMenu(wrapper, view, 0, R.attr.popupMenuStyle, R.style.PopupMenu)
                popup.menuInflater.inflate(R.menu.menu_item_manga_file, popup.menu)

                if (manga.favorite)
                    popup.menu.findItem(R.id.menu_item_manga_file_favorite).title =
                        getString(R.string.manga_library_menu_favorite_remove)
                else
                    popup.menu.findItem(R.id.menu_item_manga_file_favorite).title =
                        getString(R.string.manga_library_menu_favorite_add)

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_item_manga_file_favorite -> {
                            manga.favorite = !manga.favorite
                            mViewModel.save(manga)
                            mRecyclerView.adapter?.notifyItemChanged(position)
                        }
                        R.id.menu_item_manga_file_clear -> {
                            manga.lastAccess = LocalDateTime.MIN
                            manga.bookMark = 0
                            mViewModel.clear(manga)
                            mRecyclerView.adapter?.notifyItemChanged(position)
                        }
                        R.id.menu_item_manga_file_delete -> {
                            val dialog: AlertDialog =
                                MaterialAlertDialogBuilder(
                                    requireActivity(),
                                    R.style.AppCompatAlertDialogStyle
                                )
                                    .setTitle(getString(R.string.manga_library_menu_delete))
                                    .setMessage(getString(R.string.history_delete_description) + "\n" + manga.file.name)
                                    .setPositiveButton(
                                        R.string.action_positive
                                    ) { _, _ ->
                                        mViewModel.deletePermanent(manga)
                                        mRecyclerView.adapter?.notifyItemRemoved(position)
                                    }
                                    .setNegativeButton(
                                        R.string.action_negative
                                    ) { _, _ -> }
                                    .create()
                            dialog.show()
                        }
                        R.id.menu_item_manga_file_copy_name -> FileUtil(requireContext()).copyName(manga)
                    }
                    true
                }

                popup.show()
            }
        }
        observer()
        return root
    }

    private var itemTouchHelperCallback =
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val manga = mViewModel.getAndRemove(viewHolder.bindingAdapterPosition) ?: return
                val position = viewHolder.bindingAdapterPosition
                var excluded = false
                val dialog: AlertDialog =
                    MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                        .setTitle(getString(R.string.manga_library_menu_delete))
                        .setMessage(getString(R.string.history_delete_description) + "\n" + manga.file.name)
                        .setPositiveButton(
                            R.string.action_delete
                        ) { _, _ ->
                            mViewModel.deletePermanent(manga)
                            mRecyclerView.adapter?.notifyItemRemoved(position)
                            excluded = true
                        }.setOnDismissListener {
                            if (!excluded) {
                                mViewModel.add(manga, position)
                                mRecyclerView.adapter?.notifyItemChanged(position)
                            }
                        }
                        .create()
                dialog.show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val historyAdapter = HistoryCardAdapter()
        mRecyclerView.adapter = historyAdapter
        mRecyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
        historyAdapter.attachListener(mListener)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        mViewModel.list {
            if (it > -1)
                mRecyclerView.adapter?.notifyItemChanged(0, it)
            else
                mRecyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun updateList(list: ArrayList<Manga>) {
        (mRecyclerView.adapter as HistoryCardAdapter).updateList(list)
    }

    private fun observer() {
        mViewModel.listMangas.observe(viewLifecycleOwner) {
            updateList(it)
        }
    }

}