package br.com.fenix.bilingualreader.view.ui.popup

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Tags
import br.com.fenix.bilingualreader.service.listener.TagsListener
import br.com.fenix.bilingualreader.service.repository.TagsRepository
import br.com.fenix.bilingualreader.view.adapter.popup.TagsAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

class PopupTags(var context: Context) : TagsListener {

    private val mRepository = TagsRepository(context)
    private var mTags = mutableListOf<Tags>()
    private lateinit var mList : ListView

    private lateinit var mPopupTags : AlertDialog
    fun getPopupTags(book: Book, onClose: () -> (Unit)) {
        mPopupTags = MaterialAlertDialogBuilder(context, R.style.AppCompatMaterialAlertDialog)
            .setView(createTagsPopup(context, LayoutInflater.from(context), book.tags))
            .setCancelable(true)
            .setNeutralButton(R.string.book_tags_new) { _, _ -> getPopupTag() }
            .setNegativeButton(R.string.action_cancel) { _, _ -> onClose()}
            .setPositiveButton(R.string.action_confirm) { _, _ ->
                book.tags = mTags.filter { it.isSelected }.map { it.id!! }.toMutableList()
                mRepository.save(book)
                onClose()
            }
            .create()
        mPopupTags.show()
    }

    private fun createTagsPopup(
        context: Context,
        inflater: LayoutInflater,
        tags: MutableList<Long>
    ): View? {
        val root = inflater.inflate(R.layout.popup_tags, null, false)

        mTags = mRepository.list()

        for (tag in mTags)
            tag.isSelected = tags.any { it.compareTo(tag.id!!) == 0 }

        mList = root.findViewById(R.id.popup_tags_list)
        mList.adapter = TagsAdapter(context, R.layout.list_line_tag, mTags.toList(), this)

        return root
    }

    private lateinit var mPopupNew : AlertDialog
    private fun getPopupTag() {
        mPopupNew = MaterialAlertDialogBuilder(context, R.style.AppCompatMaterialAlertDialog)
            .setView(createTagPopup(LayoutInflater.from(context)))
            .setCancelable(false)
            .setNegativeButton(R.string.book_tags_return) { _, _ -> }
            .setPositiveButton(R.string.book_tags_add, null)
            .setOnDismissListener { mPopupTags.show() }
            .create()
        mPopupNew.show()
        mPopupNew.getButton(DialogInterface.BUTTON_POSITIVE).setOnTouchListener { _, _ ->
            if (validate()) {
                save(Tags(null, mNewTag.editText!!.text!!.toString()))
                mNewTag.isErrorEnabled = false
                mNewTag.error = ""
                mNewTag.editText?.setText("")
            }
            true
        }
    }

    private lateinit var mNewTag: TextInputLayout
    private fun createTagPopup(
        inflater: LayoutInflater
    ): View? {
        val root = inflater.inflate(R.layout.popup_tag, null, false)

        mNewTag = root.findViewById(R.id.popup_tag_text)

        return root
    }

    private fun validate(): Boolean {
        val validated: Boolean

        if (mNewTag.editText?.text == null || mNewTag.editText?.text?.toString()?.isEmpty() == true) {
            validated = false
            mNewTag.isErrorEnabled = true
            mNewTag.error = context.getString(R.string.book_tag_valid_empty)
        } else {
            validated = valid(mNewTag.editText!!.text!!.toString())
            if (!validated) {
                mNewTag.isErrorEnabled = true
                mNewTag.error = context.getString(R.string.book_tag_valid_used)
            }
        }

        return validated
    }

    override fun onCheckedChange(tag: Tags) {
        mTags.forEach {
            if (it.id!!.compareTo(tag.id!!) == 0)
                it.isSelected = tag.isSelected
        }
    }

    override fun onDelete(tag: Tags, view: View, position: Int) {
        mTags.remove(tag)
        mRepository.delete(tag)
        (mList.adapter as TagsAdapter).remove(tag)
        (mList.adapter as TagsAdapter).notifyDataSetChanged()
    }

    override fun valid(name: String): Boolean {
        return mRepository.valid(name)
    }

    override fun save(tag: Tags) {
        tag.id = mRepository.save(tag)
        mTags.add(tag)
        (mList.adapter as TagsAdapter).add(tag)
        (mList.adapter as TagsAdapter).notifyDataSetChanged()
    }
}