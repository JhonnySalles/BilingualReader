package br.com.fenix.bilingualreader.service.listener

import android.graphics.Point
import android.view.View
import br.com.fenix.bilingualreader.model.entity.PageLink
import br.com.fenix.bilingualreader.model.enums.PageLinkType

interface PageLinkCardListener {
    fun onClick(view: View, page: PageLink, isManga : Boolean, isRight: Boolean)
    fun onClickLong(view : View, page: PageLink, origin : PageLinkType, position: Int) : Boolean
    fun onDoubleClick(view: View, page: PageLink, isManga : Boolean, isRight: Boolean)
    fun onDropItem(origin : PageLinkType, destiny : PageLinkType, dragIndex: String, drop: PageLink)
    fun onDragScrolling(pointScreen : Point)
    fun onAddNotLink(page: PageLink, isRight: Boolean)
}