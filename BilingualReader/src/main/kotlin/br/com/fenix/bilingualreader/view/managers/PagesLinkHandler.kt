package br.com.fenix.bilingualreader.view.managers

import android.os.Handler
import android.os.Looper
import android.os.Message
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.PageLinkType
import br.com.fenix.bilingualreader.util.constants.PageLinkConsts
import br.com.fenix.bilingualreader.view.ui.pages_link.PagesLinkViewModel
import java.lang.ref.WeakReference

class PagesLinkHandler(listener: Listener) : Handler(Looper.getMainLooper()) {

    interface Listener {
        fun onImageStart()
        fun onImageUpdated(type: PageLinkType, index: Int?)
        fun onImageLoadError(type: PageLinkType)
        fun onEnableManualReload()
        fun onImageAdded(type: PageLinkType, index: Int?)
        fun onImageRemoved(type: PageLinkType, index: Int?)
        fun onImageFinished()
        fun onAllImagesLoaded(type: PageLinkType)
        fun onItemChange(type: PageLinkType, index: Int?)
        fun onItemAdd(type: PageLinkType, index: Int?)
        fun onItemRemove(type: PageLinkType, index: Int?)
        fun onProcessImagesStart()
        fun onProcessImagesFinished(messageResId: Int)
        fun onUndoLastChangeFinished()
    }

    private val mListenerRef: WeakReference<Listener> = WeakReference(listener)

    override fun handleMessage(msg: Message) {
        val listener = mListenerRef.get() ?: return
        val imageLoad = msg.obj as? PagesLinkViewModel.ImageLoad ?: return

        when (msg.what) {
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_START -> listener.onImageStart()
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_UPDATED -> listener.onImageUpdated(imageLoad.type, imageLoad.index)
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_LOAD_ERROR -> listener.onImageLoadError(imageLoad.type)
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_LOAD_ERROR_ENABLE_MANUAL -> listener.onEnableManualReload()
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_ADDED -> listener.onImageAdded(imageLoad.type, imageLoad.index)
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_REMOVED -> listener.onImageRemoved(imageLoad.type, imageLoad.index)
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_IMAGE_FINISHED -> listener.onImageFinished()
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ALL_IMAGES_LOADED -> listener.onAllImagesLoaded(imageLoad.type)
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_CHANGE -> listener.onItemChange(imageLoad.type, imageLoad.index)
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_ADD -> listener.onItemAdd(imageLoad.type, imageLoad.index)
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_ITEM_REMOVE -> listener.onItemRemove(imageLoad.type, imageLoad.index)
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_AUTO_PAGES_START,
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_DOUBLE_PAGES_START,
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_SIMPLE_PAGES_START,
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_SORTED_PAGES_START,
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_RETURN_PAGES_START,
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_GET_NOT_LINKED_START,
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_UNDO_LAST_CHANGE_START -> listener.onProcessImagesStart()
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_AUTO_PAGES_FINISHED ->
                listener.onProcessImagesFinished(R.string.page_link_process_reorder_auto_done)
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_DOUBLE_PAGES_FINISHED ->
                listener.onProcessImagesFinished(R.string.page_link_process_reorder_dual_pages_done)
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_SIMPLE_PAGES_FINISHED ->
                listener.onProcessImagesFinished(R.string.page_link_process_reorder_single_page_done)
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_SORTED_PAGES_FINISHED,
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_RETURN_PAGES_FINISHED,
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_REORDER_GET_NOT_LINKED_FINISHED ->
                listener.onProcessImagesFinished(R.string.page_link_process_reorder_sorted_page_done)
            PageLinkConsts.MESSAGES.MESSAGE_PAGES_LINK_UNDO_LAST_CHANGE_FINISHED -> listener.onUndoLastChangeFinished()
        }
    }
}
