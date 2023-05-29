package br.com.fenix.bilingualreader.service.listener

import android.graphics.Point
import android.view.View
import br.com.fenix.bilingualreader.model.entity.LinkedPage
import br.com.fenix.bilingualreader.model.enums.PageLinkType

interface PageLinkCardListener {
    fun onClick(view: View, page: LinkedPage, isManga : Boolean, isRight: Boolean)
    fun onClickLong(view : View, page: LinkedPage, origin : PageLinkType, position: Int) : Boolean
    fun onDoubleClick(view: View, page: LinkedPage, isManga : Boolean, isRight: Boolean)
    fun onDropItem(origin : PageLinkType, destiny : PageLinkType, dragIndex: String, drop: LinkedPage)
    fun onDragScrolling(pointScreen : Point)
    fun onAddNotLink(page: LinkedPage, isRight: Boolean)
}