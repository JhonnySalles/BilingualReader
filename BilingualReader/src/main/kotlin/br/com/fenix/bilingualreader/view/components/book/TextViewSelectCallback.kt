package br.com.fenix.bilingualreader.view.components.book

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.text.Selection
import android.text.Spannable
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.BookAnnotation
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.service.listener.TextSelectCallbackListener
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import br.com.fenix.bilingualreader.util.helpers.PopupUtil
import br.com.fenix.bilingualreader.view.ui.popup.PopupTextSelect


class TextViewSelectCallback(val context: Context, val holder: TextViewPager.TextViewPagerHolder, val page: Int, val createSpan: (annotation: BookAnnotation, start: Int, end: Int) -> (Unit), val listener: TextSelectCallbackListener?) : ActionMode.Callback {

    private val mTextView: TextView = holder.textView
    private val mMenuCustom : PopupTextSelect = PopupTextSelect(holder.popupTextSelect, this)
    
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        if (ReaderConsts.READER.BOOK_NATIVE_POPUP_MENU_SELECT) {
            //mode?.title = "Menu action"
            mode?.menu?.setGroupDividerEnabled(true)
            mode?.menuInflater?.inflate(R.menu.menu_text_view_select, menu)
        } else
            mMenuCustom.setAction(mode)
        
        return true;
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        if (ReaderConsts.READER.BOOK_NATIVE_POPUP_MENU_SELECT) {
            menu?.removeItem(android.R.id.cut)
            menu?.removeItem(android.R.id.shareText)
        } else
            menu?.clear()
        return true;
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {

        var start: Int = mTextView.selectionStart
        var end: Int = mTextView.selectionEnd

        if (mTextView.selectionEnd < mTextView.selectionStart) {
            start = mTextView.selectionEnd
            end = mTextView.selectionStart
        }

        if (item != null)
            when (item.itemId) {
                R.id.popup_text_select_functions_copy, android.R.id.copy -> {
                    copyText(mTextView.text.substring(start, end))
                    mode?.finish()
                }

                R.id.popup_text_select_functions_select_all, android.R.id.selectAll -> selectAllText()

                R.id.popup_text_select_functions_tts, R.id.menu_text_select_functions_tts -> {
                    listener?.textSelectReadingFrom(page, mTextView.text.substring(start, end))
                    mode?.finish()
                }

                R.id.popup_text_select_green, R.id.menu_text_select_colors_green -> {
                    listener?.textSelectAddMark(page, mTextView.text.substring(start, end), Color.Green, start, end)?.let {
                        createSpan(it, start, end)
                    }
                    mode?.finish()
                    return true
                }

                R.id.popup_text_select_red, R.id.menu_text_select_colors_red -> {
                    listener?.textSelectAddMark(page, mTextView.text.substring(start, end), Color.Red, start, end)?.let {
                        createSpan(it, start, end)
                    }
                    mode?.finish()
                    return true
                }

                R.id.popup_text_select_yellow, R.id.menu_text_select_colors_yellow -> {
                    listener?.textSelectAddMark(page, mTextView.text.substring(start, end), Color.Yellow, start, end)?.let {
                        createSpan(it, start, end)
                    }
                    mode?.finish()
                    return true
                }

                R.id.popup_text_select_blue, R.id.menu_text_select_colors_blue -> {
                    listener?.textSelectAddMark(page, mTextView.text.substring(start, end), Color.Blue, start, end)?.let {
                        createSpan(it, start, end)
                    }
                    mode?.finish()
                    return true
                }

                R.id.popup_text_select_erase, R.id.menu_text_select_colors_erase -> {
                    listener?.textSelectRemoveMark(page, start, end)
                    mode?.finish()
                    return true
                }

                R.id.popup_text_select_functions_search -> {
                    listener?.textSearch(page, mTextView.text.substring(start, end))
                    mode?.finish()
                }

                R.id.popup_text_select_functions_translate -> {
                    translateText(mTextView.text.substring(start, end))
                    mode?.finish()
                }
            }
        return false
    }

    private fun selectAllText() {
        Selection.setSelection(mTextView.text as Spannable, 0, mTextView.length())
    }

    private fun copyText(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, context.getString(R.string.action_copy, text), Toast.LENGTH_LONG).show()
    }

    private fun translateText(text: String) = PopupUtil.googleTranslate(context, text)

    override fun onDestroyActionMode(mode: ActionMode?) {
        try {
            if (!ReaderConsts.READER.BOOK_NATIVE_POPUP_MENU_SELECT)
                holder.popupTextSelect.dismiss()
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }

}