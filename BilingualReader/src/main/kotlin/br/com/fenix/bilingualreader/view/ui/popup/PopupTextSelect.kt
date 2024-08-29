package br.com.fenix.bilingualreader.view.ui.popup

import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.view.ActionMode
import android.view.ActionProvider
import android.view.ContextMenu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import android.widget.PopupWindow
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.util.constants.ReaderConsts
import com.google.android.material.button.MaterialButton


class PopupTextSelect(popup: PopupWindow, callback: ActionMode.Callback) {

    private var mActionMode: ActionMode? = null

    init {
        if (!ReaderConsts.READER.BOOK_NATIVE_POPUP_MENU_SELECT) {
            val content = popup.contentView
            generateClick(content.findViewById(R.id.popup_text_select_functions_select_all), callback, true)
            generateClick(content.findViewById(R.id.popup_text_select_functions_copy), callback, true)
            generateClick(content.findViewById(R.id.popup_text_select_functions_search), callback, true)
            generateClick(content.findViewById(R.id.popup_text_select_functions_translate), callback)
            generateClick(content.findViewById(R.id.popup_text_select_functions_tts), callback, true)

            generateClick(content.findViewById(R.id.popup_text_select_erase), callback, true)
            generateClick(content.findViewById(R.id.popup_text_select_red), callback)
            generateClick(content.findViewById(R.id.popup_text_select_blue), callback)
            generateClick(content.findViewById(R.id.popup_text_select_green), callback)
            generateClick(content.findViewById(R.id.popup_text_select_yellow), callback)
        }
    }

    fun setAction(action: ActionMode?) {
        mActionMode = action
    }

    private fun generateClick(button: MaterialButton, callback: ActionMode.Callback, isIconAnimated: Boolean = false) {
        button.setOnClickListener {
            if (isIconAnimated)
                (button.icon as AnimatedVectorDrawable).start()

            callback.onActionItemClicked(mActionMode, PopupTextSelectMenuItem(button.id))
        }
    }

    inner class PopupTextSelectMenuItem(private val itemId: Int) : MenuItem {
        override fun getItemId(): Int = itemId

        override fun getGroupId(): Int {
            TODO("Not yet implemented")
        }

        override fun getOrder(): Int {
            TODO("Not yet implemented")
        }

        override fun setTitle(p0: CharSequence?): MenuItem {
            TODO("Not yet implemented")
        }

        override fun setTitle(p0: Int): MenuItem {
            TODO("Not yet implemented")
        }

        override fun getTitle(): CharSequence? {
            TODO("Not yet implemented")
        }

        override fun setTitleCondensed(p0: CharSequence?): MenuItem {
            TODO("Not yet implemented")
        }

        override fun getTitleCondensed(): CharSequence? {
            TODO("Not yet implemented")
        }

        override fun setIcon(p0: Drawable?): MenuItem {
            TODO("Not yet implemented")
        }

        override fun setIcon(p0: Int): MenuItem {
            TODO("Not yet implemented")
        }

        override fun getIcon(): Drawable? {
            TODO("Not yet implemented")
        }

        override fun setIntent(p0: Intent?): MenuItem {
            TODO("Not yet implemented")
        }

        override fun getIntent(): Intent? {
            TODO("Not yet implemented")
        }

        override fun setShortcut(p0: Char, p1: Char): MenuItem {
            TODO("Not yet implemented")
        }

        override fun setNumericShortcut(p0: Char): MenuItem {
            TODO("Not yet implemented")
        }

        override fun getNumericShortcut(): Char {
            TODO("Not yet implemented")
        }

        override fun setAlphabeticShortcut(p0: Char): MenuItem {
            TODO("Not yet implemented")
        }

        override fun getAlphabeticShortcut(): Char {
            TODO("Not yet implemented")
        }

        override fun setCheckable(p0: Boolean): MenuItem {
            TODO("Not yet implemented")
        }

        override fun isCheckable(): Boolean {
            TODO("Not yet implemented")
        }

        override fun setChecked(p0: Boolean): MenuItem {
            TODO("Not yet implemented")
        }

        override fun isChecked(): Boolean {
            TODO("Not yet implemented")
        }

        override fun setVisible(p0: Boolean): MenuItem {
            TODO("Not yet implemented")
        }

        override fun isVisible(): Boolean {
            TODO("Not yet implemented")
        }

        override fun setEnabled(p0: Boolean): MenuItem {
            TODO("Not yet implemented")
        }

        override fun isEnabled(): Boolean {
            TODO("Not yet implemented")
        }

        override fun hasSubMenu(): Boolean {
            TODO("Not yet implemented")
        }

        override fun getSubMenu(): SubMenu? {
            TODO("Not yet implemented")
        }

        override fun setOnMenuItemClickListener(p0: MenuItem.OnMenuItemClickListener?): MenuItem {
            TODO("Not yet implemented")
        }

        override fun getMenuInfo(): ContextMenu.ContextMenuInfo? {
            TODO("Not yet implemented")
        }

        override fun setShowAsAction(p0: Int) {
            TODO("Not yet implemented")
        }

        override fun setShowAsActionFlags(p0: Int): MenuItem {
            TODO("Not yet implemented")
        }

        override fun setActionView(p0: View?): MenuItem {
            TODO("Not yet implemented")
        }

        override fun setActionView(p0: Int): MenuItem {
            TODO("Not yet implemented")
        }

        override fun getActionView(): View? {
            TODO("Not yet implemented")
        }

        override fun setActionProvider(p0: ActionProvider?): MenuItem {
            TODO("Not yet implemented")
        }

        override fun getActionProvider(): ActionProvider? {
            TODO("Not yet implemented")
        }

        override fun expandActionView(): Boolean {
            TODO("Not yet implemented")
        }

        override fun collapseActionView(): Boolean {
            TODO("Not yet implemented")
        }

        override fun isActionViewExpanded(): Boolean {
            TODO("Not yet implemented")
        }

        override fun setOnActionExpandListener(p0: MenuItem.OnActionExpandListener?): MenuItem {
            TODO("Not yet implemented")
        }

    }

}