package br.com.fenix.bilingualreader.view.adapter.page_link

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.PageLink
import br.com.fenix.bilingualreader.model.enums.PageLinkType
import br.com.fenix.bilingualreader.service.listener.PageLinkCardListener
import com.google.android.material.card.MaterialCardView


class PageNotLinkViewHolder(itemView: View, private val listener: PageLinkCardListener) : RecyclerView.ViewHolder(itemView) {

    fun bind(page: PageLink) {
        val root = itemView.findViewById<MaterialCardView>(R.id.page_not_link_card)
        val image = itemView.findViewById<ImageView>(R.id.page_not_link_image)
        if (page.imageLeftFileLinkPage != null)
            image.setImageBitmap(page.imageLeftFileLinkPage)

        root.setOnClickListener { listener.onDoubleClick(root, page, isManga = false, isRight = false) }
        root.setOnLongClickListener { listener.onClickLong(it, page, PageLinkType.NOT_LINKED, -1) }
    }

}