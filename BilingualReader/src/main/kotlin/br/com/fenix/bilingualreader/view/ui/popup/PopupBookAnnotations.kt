package br.com.fenix.bilingualreader.view.ui.popup

import android.content.Context
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.entity.Tags
import br.com.fenix.bilingualreader.service.repository.BookAnnotationRepository
import com.google.android.material.textfield.TextInputLayout


class PopupBookAnnotations(var context: Context) {

    companion object {
        fun generateClick(annotation: BookAnnotation, color: Int, click: (String) -> (Unit)): ClickableSpan {
            return object : ClickableSpan() {
                override fun onClick(p0: View) {
                    //click(text)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                    ds.color = color
                }
            }
        }
    }

    private val mRepository = BookAnnotationRepository(context)
    private var mTags = mutableListOf<Tags>()
    private lateinit var mList : ListView

    private lateinit var mPopupTags : AlertDialog

    private fun createTagsPopup(context: Context, inflater: LayoutInflater, tags: MutableList<Long>): View? {
        val root = inflater.inflate(R.layout.popup_tags, null, false)

        return root
    }

    private lateinit var mNewTag: TextInputLayout
    private fun createTagPopup(inflater: LayoutInflater): View? {
        val root = inflater.inflate(R.layout.popup_tag, null, false)
        mNewTag = root.findViewById(R.id.popup_tag_text)
        return root
    }

}