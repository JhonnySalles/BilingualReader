package br.com.fenix.bilingualreader.view.ui.reader.manga

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.MangaAnnotation
import br.com.fenix.bilingualreader.service.listener.MangaAnnotationListener
import br.com.fenix.bilingualreader.service.listener.ReaderListener
import br.com.fenix.bilingualreader.view.adapter.reader.MangaAnnotationsCardAdapter
import kotlin.math.max


class PopupMangaAnnotationsFragment : Fragment() {

    private val mViewModel: MangaReaderViewModel by activityViewModels()

    private lateinit var mAnnotationList: RecyclerView
    private var mListener : ReaderListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAnnotationList = view.findViewById(R.id.popup_manga_annotations_list)
        prepareAnnotations()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.popup_manga_annotations, container, false)
    }

    fun setListener(listener: ReaderListener?) {
        mListener = listener
    }

    fun clearListener() {
        mListener = null
    }

    private fun prepareAnnotations() {
        val lineAdapter = MangaAnnotationsCardAdapter()
        mAnnotationList.adapter = lineAdapter
        val width = resources.getDimension(R.dimen.manga_annotation_layout_width).toInt()
        val count: Int = max(1, (Resources.getSystem().displayMetrics.widthPixels -3) / width)
        mAnnotationList.layoutManager = StaggeredGridLayoutManager(count, StaggeredGridLayoutManager.VERTICAL)
        mAnnotationList.layoutAnimation = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.animation_manga_annotation_grid)

        val itemTouchHelperCallback =
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(recyclerView: RecyclerView, viewHolder: ViewHolder, target: ViewHolder): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
                    mViewModel.deleteAnnotation(viewHolder.bindingAdapterPosition)
                }
            }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mAnnotationList)

        val listener = object : MangaAnnotationListener {
            override fun onClick(annotation: MangaAnnotation) {
                mListener?.setCurrentPage(annotation.page)
            }
        }

        lineAdapter.attachListener(listener)
        mViewModel.annotation.observe(viewLifecycleOwner) {
            lineAdapter.updateList(it)
        }
    }

    fun notifyItemChanged(page: Int) = mAnnotationList.adapter?.notifyItemChanged(page)

}