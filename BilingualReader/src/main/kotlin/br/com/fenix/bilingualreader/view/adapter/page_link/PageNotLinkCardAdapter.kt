package br.com.fenix.bilingualreader.view.adapter.page_link

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.LinkedPage
import br.com.fenix.bilingualreader.service.listener.PageLinkCardListener

class PageNotLinkCardAdapter : RecyclerView.Adapter<PageNotLinkViewHolder>() {

    private lateinit var mListener: PageLinkCardListener
    private var mPageNotLinkList: ArrayList<LinkedPage> = arrayListOf()

    override fun onBindViewHolder(holder: PageNotLinkViewHolder, position: Int) {
        holder.bind(mPageNotLinkList[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageNotLinkViewHolder {
        return PageNotLinkViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.grid_card_page_not_link, parent, false), mListener)
    }

    override fun getItemCount(): Int {
        return mPageNotLinkList.size
    }

    fun updateList(list: ArrayList<LinkedPage>) {
        mPageNotLinkList = list
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