package br.com.fenix.bilingualreader.view.adapter.page_link

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.PageLink
import br.com.fenix.bilingualreader.service.listener.PageLinkCardListener

class PageLinkCardAdapter : RecyclerView.Adapter<PageLinkViewHolder>() {

    private lateinit var mListener: PageLinkCardListener
    private var mPageLinkList: ArrayList<PageLink> = arrayListOf()

    override fun onBindViewHolder(holder: PageLinkViewHolder, position: Int) {
        holder.bind(mPageLinkList[position], position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageLinkViewHolder {
        return PageLinkViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.grid_card_page_link, parent, false), mListener)
    }

    override fun getItemCount(): Int {
        return mPageLinkList.size
    }

    fun updateList(list: ArrayList<PageLink>) {
        mPageLinkList = list
        notifyDataSet()
    }

    fun attachListener(listener: PageLinkCardListener) {
        mListener = listener
    }

    private fun notifyDataSet(idItem: Int? = null) {
        if (idItem != null)
            notifyItemChanged(idItem)
        else
            notifyDataSetChanged()
    }

}