package br.com.fenix.bilingualreader.view.components.book

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.text.Selection
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.service.listener.TextSelectCallbackListener


class TextViewSelectCallback(val context: Context, val textView: TextView, val page: Int, val listener: TextSelectCallbackListener?) : ActionMode.Callback {

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        //mode?.title = "Menu action"
        mode?.menu?.setGroupDividerEnabled(true)
        mode?.menuInflater?.inflate(R.menu.menu_text_view_select, menu)
        return true;
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu?.removeItem(android.R.id.cut)
        menu?.removeItem(android.R.id.shareText)
        return true;
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {

        val start: Int = textView.selectionStart
        val end: Int = textView.selectionEnd
        val wordtoSpan = textView.text as Spannable

        if (item != null)
            when (item.itemId) {
                android.R.id.copy -> {
                    copyText(textView.text.substring(start, end))
                    mode?.finish()
                }

                android.R.id.selectAll -> selectAllText()
                R.id.menu_text_select_functions_tts -> {
                    listener?.textSelectReadingFrom(page, textView.text.substring(start, end))
                    mode?.finish()
                }

                R.id.menu_text_select_colors_green -> {
                    val color = Color.Green
                    wordtoSpan.setSpan(
                        BackgroundColorSpan(context.getColor(color.getColor())), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    listener?.textSelectAddMark(page, textView.text.substring(start, end), color, start, end)
                    return true
                }

                R.id.menu_text_select_colors_red -> {
                    val color = Color.Red
                    wordtoSpan.setSpan(
                        BackgroundColorSpan(context.getColor(color.getColor())), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    listener?.textSelectAddMark(page, textView.text.substring(start, end), color, start, end)
                    return true
                }

                R.id.menu_text_select_colors_yellow -> {
                    val color = Color.Yellow
                    wordtoSpan.setSpan(
                        BackgroundColorSpan(context.getColor(color.getColor())), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    listener?.textSelectAddMark(page, textView.text.substring(start, end), color, start, end)
                    return true
                }

                R.id.menu_text_select_colors_blue -> {
                    val color = Color.Blue
                    wordtoSpan.setSpan(
                        BackgroundColorSpan(context.getColor(color.getColor())), start, end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    listener?.textSelectAddMark(page, textView.text.substring(start, end), color, start, end)
                    return true
                }

                R.id.menu_text_select_colors_erase -> {
                    wordtoSpan.setSpan(BackgroundColorSpan(android.graphics.Color.TRANSPARENT), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    listener?.textSelectRemoveMark(page, start, end)
                    return true
                }
            }
        return false
    }

    private fun selectAllText() {
        Selection.setSelection(textView.getText() as Spannable, 0, textView.length())
    }

    private fun copyText(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, context.getString(R.string.action_copy, text), Toast.LENGTH_LONG).show()
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
    }

}