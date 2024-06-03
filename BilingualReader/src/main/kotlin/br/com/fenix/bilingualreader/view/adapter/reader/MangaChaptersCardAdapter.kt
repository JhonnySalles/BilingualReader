package br.com.fenix.bilingualreader.view.adapter.reader

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Chapters
import br.com.fenix.bilingualreader.service.listener.ChapterCardListener

class MangaChaptersCardAdapter : RecyclerView.Adapter<MangaChaptersViewHolder>() {

    private lateinit var mListener: ChapterCardListener
    private var mList: List<Chapters> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MangaChaptersViewHolder {
        val item =
            LayoutInflater.from(parent.context).inflate(R.layout.grid_card_manga_chapter, parent, false)
        return MangaChaptersViewHolder(item, mListener)
    }

    override fun onBindViewHolder(holder: MangaChaptersViewHolder, position: Int) {
        holder.bind(mList[position])
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    fun attachListener(listener: ChapterCardListener) {
        mListener = listener
    }

    fun updateList(list: List<Chapters>) {
        mList = list
        notifyDataSetChanged()
    }

}