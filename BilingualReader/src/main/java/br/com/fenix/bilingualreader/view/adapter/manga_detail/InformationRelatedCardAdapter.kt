package br.com.fenix.bilingualreader.view.adapter.manga_detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Information
import br.com.fenix.bilingualreader.service.listener.InformationCardListener


class InformationRelatedCardAdapter : RecyclerView.Adapter<InformationRelatedViewHolder>() {

    private lateinit var mListener: InformationCardListener
    private var mPageLinkList: MutableList<Information> = mutableListOf()

    override fun onBindViewHolder(holder: InformationRelatedViewHolder, position: Int) {
        holder.bind(mPageLinkList[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InformationRelatedViewHolder {
        return InformationRelatedViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.related_card_manga_detail_information, parent, false),
            mListener
        )
    }

    fun attachListener(listener: InformationCardListener) {
        mListener = listener
    }

    override fun getItemCount(): Int {
        return mPageLinkList.size
    }

    fun updateList(list: MutableList<Information>?) {
        mPageLinkList = list ?: mutableListOf()
        notifyDataSet()
    }

    private fun notifyDataSet(idItem: Int? = null) {
        if (idItem != null)
            notifyItemChanged(idItem)
        else
            notifyDataSetChanged()
    }

}