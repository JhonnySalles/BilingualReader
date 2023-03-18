package br.com.fenix.bilingualreader.view.ui.book

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.EditText
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.enums.ListMode
import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.service.listener.BookAnnotationListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.adapter.book.BookAnnotationLineAdapter
import br.com.fenix.bilingualreader.view.adapter.vocabulary.VocabularyMangaListCardAdapter
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.slf4j.LoggerFactory


class BookAnnotationFragment : Fragment() {

    private val mLOGGER = LoggerFactory.getLogger(BookAnnotationFragment::class.java)

    private val mViewModel: BookAnnotationViewModel by activityViewModels()

    private lateinit var mScrollUp: FloatingActionButton
    private lateinit var mScrollDown: FloatingActionButton
    private lateinit var mRecyclerView: RecyclerView

    private lateinit var mListener: BookAnnotationListener

    private lateinit var mBook: Book

    private var mHandler = Handler(Looper.getMainLooper())
    private val mDismissUpButton = Runnable { mScrollUp.hide() }
    private val mDismissDownButton = Runnable { mScrollDown.hide() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        arguments?.let {
            mBook = it.getSerializable(GeneralConsts.KEYS.OBJECT.BOOK) as Book
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_book_mark, menu)
        super.onCreateOptionsMenu(menu, inflater)

    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_history_manga_library -> {}
        }
        return super.onOptionsItemSelected(menuItem)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_book_annotation, container, false)

        mRecyclerView = root.findViewById(R.id.book_annotation_recycler_view)

        mScrollUp = root.findViewById(R.id.book_annotation_scroll_up)
        mScrollDown = root.findViewById(R.id.book_annotation_scroll_down)

        mScrollUp.visibility = View.GONE
        mScrollDown.visibility = View.GONE

        mScrollUp.setOnClickListener {
            (mScrollUp.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.smoothScrollToPosition(0)
        }
        mScrollDown.setOnClickListener {
            (mScrollDown.drawable as AnimatedVectorDrawable).start()
            mRecyclerView.smoothScrollToPosition((mRecyclerView.adapter as RecyclerView.Adapter).itemCount)
        }

        mRecyclerView.setOnScrollChangeListener { _, _, _, _, yOld ->
            if (yOld > 20) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mDismissDownButton))
                        mHandler.removeCallbacks(mDismissDownButton)
                } else
                    mHandler.removeCallbacks(mDismissDownButton)

                mScrollDown.hide()
            } else if (yOld < -20) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mDismissUpButton))
                        mHandler.removeCallbacks(mDismissUpButton)
                } else
                    mHandler.removeCallbacks(mDismissUpButton)

                mScrollUp.hide()
            }

            if (yOld > 150) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mDismissUpButton))
                        mHandler.removeCallbacks(mDismissUpButton)
                } else
                    mHandler.removeCallbacks(mDismissUpButton)

                mHandler.postDelayed(mDismissUpButton, 3000)
                mScrollUp.show()
            } else if (yOld < -150) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (mHandler.hasCallbacks(mDismissDownButton))
                        mHandler.removeCallbacks(mDismissDownButton)
                } else
                    mHandler.removeCallbacks(mDismissDownButton)

                mHandler.postDelayed(mDismissDownButton, 3000)
                mScrollDown.show()
            }
        }

        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mListener = object : BookAnnotationListener {
            override fun onClick(annotation: BookAnnotation) {
                val bundle = Bundle()
                bundle.putSerializable(GeneralConsts.KEYS.OBJECT.BOOK_ANNOTATION, annotation)
                (requireActivity() as MenuActivity).onBack(bundle)
            }

            override fun onClickFavorite(annotation: BookAnnotation) {
                mViewModel.save(annotation)
            }

            override fun onClickOptions(annotation: BookAnnotation, view: View, position: Int) {
                val wrapper = ContextThemeWrapper(requireContext(), R.style.PopupMenu)
                val popup = PopupMenu(wrapper, view, 0, R.attr.popupMenuStyle, R.style.PopupMenu)
                popup.menuInflater.inflate(R.menu.menu_item_book_annotation, popup.menu)

                if (annotation.type != MarkType.Annotation)
                    popup.menu.removeItem(R.id.menu_item_item_book_annotation_change_detach)

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_item_book_annotation_favorite -> {
                            annotation.favorite = !annotation.favorite
                            mViewModel.save(annotation)
                            notifyDataSet(position)
                        }
                        R.id.menu_item_item_book_annotation_delete -> {
                            deleteAnnotation(annotation, position)
                        }
                        R.id.menu_item_item_book_annotation_change_detach -> {
                            val colors = Util.getColors(requireContext())
                            val items = colors.keys.toTypedArray()

                            MaterialAlertDialogBuilder(
                                requireActivity(),
                                R.style.AppCompatAlertDialogStyle
                            )
                                .setTitle(getString(R.string.book_annotation_change_detach_title))
                                .setMessage(
                                    getString(
                                        R.string.book_annotation_change_detach_description
                                    )
                                )
                                .setItems(items) { _, selected ->
                                    val color = colors[items[selected]]
                                    if (color != null) {
                                        annotation.color = color
                                        mViewModel.save(annotation)
                                        notifyDataSet(position)
                                    }
                                }
                                .show()
                        }
                    }
                    true
                }

                popup.show()
            }

            override fun onClickNote(annotation: BookAnnotation) {
                MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                    .setTitle(getString(R.string.book_annotation_note))
                    .setView(R.layout.dialog_add_note)
                    .setPositiveButton(getString(R.string.action_confirm)) { dialog, _ ->
                        (dialog as? AlertDialog)?.findViewById<EditText>(R.id.book_note_text)?.let {
                            annotation.annotation = it.text.toString()
                            mViewModel.save(annotation)
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }

        }

        val adapter = BookAnnotationLineAdapter()
        adapter.attachListener(mListener)
        mRecyclerView.adapter = adapter
        mRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecyclerView)

        observer()
    }

    private fun observer() {
        mViewModel.annotation.observe(viewLifecycleOwner) {
            (mRecyclerView.adapter as BookAnnotationLineAdapter).updateList(it)
        }
    }

    private fun notifyDataSet(indexes: MutableList<Pair<ListMode, Int>>) {
        if (indexes.any { it.first == ListMode.FULL })
            notifyDataSet(0, (mViewModel.annotation.value?.size ?: 1))
        else {
            for (index in indexes)
                when (index.first) {
                    ListMode.ADD -> notifyDataSet(index.second, insert = true)
                    ListMode.REM -> notifyDataSet(index.second, removed = true)
                    ListMode.MOD -> notifyDataSet(index.second)
                    else -> notifyDataSet(index.second)
                }
        }
    }

    private fun notifyDataSet(
        index: Int,
        range: Int = 0,
        insert: Boolean = false,
        removed: Boolean = false
    ) {
        if (insert)
            mRecyclerView.adapter?.notifyItemInserted(index)
        else if (removed)
            mRecyclerView.adapter?.notifyItemRemoved(index)
        else if (range > 1)
            mRecyclerView.adapter?.notifyItemRangeChanged(index, range)
        else
            mRecyclerView.adapter?.notifyItemChanged(index)
    }

    override fun onDestroy() {
        VocabularyMangaListCardAdapter.clearVocabularyMangaList()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mHandler.hasCallbacks(mDismissUpButton))
                mHandler.removeCallbacks(mDismissUpButton)
            if (mHandler.hasCallbacks(mDismissDownButton))
                mHandler.removeCallbacks(mDismissDownButton)
        } else {
            mHandler.removeCallbacks(mDismissUpButton)
            mHandler.removeCallbacks(mDismissDownButton)
        }

        super.onDestroy()
    }


    private var itemTouchHelperCallback =
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val annotation =
                    mViewModel.getAndRemove(viewHolder.bindingAdapterPosition) ?: return
                val position = viewHolder.bindingAdapterPosition
                deleteAnnotation(annotation, position)
            }
        }

    private fun deleteAnnotation(annotation: BookAnnotation, position: Int) {
        var excluded = false
        val dialog: AlertDialog =
            MaterialAlertDialogBuilder(requireActivity(), R.style.AppCompatAlertDialogStyle)
                .setTitle(getString(R.string.book_annotation_delete))
                .setMessage(
                    getString(
                        R.string.book_annotation_delete_description,
                        getString(annotation.type.getDescription()).lowercase()
                    )
                )
                .setPositiveButton(
                    R.string.action_delete
                ) { _, _ ->
                    mViewModel.delete(annotation)
                    notifyDataSet(position, removed = true)
                    excluded = true
                }.setOnDismissListener {
                    if (!excluded) {
                        mViewModel.add(annotation, position)
                        notifyDataSet(position)
                    }
                }
                .create()
        dialog.show()
    }

}