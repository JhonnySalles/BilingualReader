package br.com.fenix.bilingualreader.view.adapter.fonts

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.enums.FontType
import br.com.fenix.bilingualreader.service.listener.FontsListener
import br.com.fenix.bilingualreader.util.helpers.ThemeUtil.ThemeUtils.getColorFromAttr

class FontsCardAdapter(var context: Context, list: MutableList<Pair<FontType, Boolean>>, listener: FontsListener) : BaseAdapter() {

    companion object {
        var mFontSelect: Int = 0
        var mFontNormal: Int = 0
    }

    init {
        mFontSelect = context.getColorFromAttr(R.attr.colorPrimary)
        mFontNormal = context.getColorFromAttr(R.attr.colorOnBackground)
    }

    private var mListener: FontsListener = listener
    private var mList: MutableList<Pair<FontType, Boolean>> = list

    fun updateList(list: MutableList<Pair<FontType, Boolean>>) {
        mList = list
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return mList.size
    }

    override fun getItem(index: Int): Pair<FontType, Boolean> {
        return mList[index]
    }

    override fun getItemId(index: Int): Long {
        return index.toLong()
    }

    override fun getView(index: Int, view: View?, parent: ViewGroup?): View? {
        val font = getItem(index)

        val newView = view ?: LayoutInflater.from(context).inflate(R.layout.line_fonts_type, parent, false)

        val example = newView.findViewById<TextView>(R.id.font_example)
        example.setTextColor(if (font.second) mFontSelect else mFontNormal)
        example.typeface = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            context.resources.getFont(font.first.getFont())
        else
            ResourcesCompat.getFont(context, font.first.getFont())

        example.text = if(font.first.isJapanese())
            context.getString(R.string.popup_reading_font_style_example_japanese)
        else
            context.getString(R.string.popup_reading_font_style_example)

        val description = newView.findViewById<TextView>(R.id.font_description)
        description?.setTextColor(if (font.second) mFontSelect else mFontNormal)
        description?.text = context.getString(font.first.getDescription())

        newView.findViewById<LinearLayout>(R.id.font_root)?.setOnClickListener {
            mListener.onClick(font)
        }

        return newView
    }
}