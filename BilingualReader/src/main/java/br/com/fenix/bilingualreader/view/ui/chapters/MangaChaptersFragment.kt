package br.com.fenix.bilingualreader.view.ui.chapters

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Chapters
import br.com.fenix.bilingualreader.service.listener.ChapterCardListener
import br.com.fenix.bilingualreader.service.listener.ChapterLoadListener
import br.com.fenix.bilingualreader.service.repository.SharedData
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.view.adapter.chapters.ChaptersLineAdapter
import br.com.fenix.bilingualreader.view.ui.menu.MenuActivity
import br.com.fenix.bilingualreader.view.ui.reader.manga.MangaReaderViewModel
import org.slf4j.LoggerFactory
import kotlin.math.max


class MangaChaptersFragment : Fragment(), ChapterLoadListener {

    private val mLOGGER = LoggerFactory.getLogger(MangaChaptersFragment::class.java)

    private val mViewModel: MangaReaderViewModel by activityViewModels()

    private lateinit var mRecyclerView: RecyclerView
    private var PosInitial = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        requireArguments().let {
            PosInitial = if (it.containsKey(GeneralConsts.KEYS.MANGA.PAGE_NUMBER)) it.getInt(GeneralConsts.KEYS.MANGA.PAGE_NUMBER) else 0
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_manga_chapters, container, false)

        mRecyclerView = root.findViewById(R.id.manga_chapters_recycler_view)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listener = object : ChapterCardListener {
            override fun onClick(page: Chapters) {
                val bundle = Bundle()
                bundle.putString(GeneralConsts.KEYS.CHAPTERS.TITLE, page.title)
                bundle.putInt(GeneralConsts.KEYS.CHAPTERS.NUMBER, page.number)
                bundle.putInt(GeneralConsts.KEYS.CHAPTERS.PAGE, page.page)
                (requireActivity() as MenuActivity).onBack(bundle)
            }
        }

        val adapter = ChaptersLineAdapter()
        adapter.attachListener(listener)
        mRecyclerView.adapter = adapter

        val count = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            max(4, (Resources.getSystem().displayMetrics.widthPixels) / resources.getDimension(R.dimen.chapters_grid_card_layout_width).toInt()) - 1
        } else 3
        mRecyclerView.layoutManager = StaggeredGridLayoutManager(count, StaggeredGridLayoutManager.VERTICAL)

        observer()
        SharedData.addListener(this)
    }

    private fun observer() {
        SharedData.chapters.observe(viewLifecycleOwner) {
            (mRecyclerView.adapter as ChaptersLineAdapter).updateList(it)
        }
        if (PosInitial > 0)
            mRecyclerView.scrollToPosition(PosInitial)
    }

    override fun onDestroy() {
        SharedData.remListener(this)
        super.onDestroy()
    }

    override fun onLoading(page: Int) {
        (mRecyclerView.adapter as ChaptersLineAdapter).notifyItemChanged(page)
    }

}