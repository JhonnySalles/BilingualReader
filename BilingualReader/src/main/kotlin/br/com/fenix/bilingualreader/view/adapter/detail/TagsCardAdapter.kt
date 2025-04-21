package br.com.fenix.bilingualreader.view.adapter.detail

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.util.helpers.ColorUtil
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

class TagsCardAdapter(var context: Context, list: MutableList<String>) : BaseAdapter() {

    companion object {
        var mRadius: Float = 0f
        var mShapeAppearanceModel : ShapeAppearanceModel? = null
    }

    init {
        mRadius = context.resources.getDimension(R.dimen.detail_local_information_tags_radius)
        mShapeAppearanceModel = ShapeAppearanceModel().toBuilder().setAllCorners(CornerFamily.ROUNDED, mRadius).build()
    }


    private var mList: MutableList<String> = list

    fun updateList(list: MutableList<String>) {
        mList = list
        notifyDataSetChanged()
    }

    fun clearList() {
        mList.clear()
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return mList.size
    }

    override fun getItem(index: Int): String {
        return mList[index]
    }

    override fun getItemId(index: Int): Long {
        return index.toLong()
    }

    override fun getView(index: Int, view: View?, parent: ViewGroup?): View {
        val theme = getItem(index)

        val newView = view ?: LayoutInflater.from(context).inflate(R.layout.list_item_tag, parent, false)

        newView?.findViewById<LinearLayout>(R.id.list_item_tag_content)?.let {
            val shapeDrawable = MaterialShapeDrawable(mShapeAppearanceModel!!)
            shapeDrawable.fillColor = ColorStateList.valueOf(ColorUtil.randomColor())
            ViewCompat.setBackground(it, shapeDrawable)
        }

        newView?.findViewById<TextView>(R.id.list_item_tag)?.text = theme

        return newView
    }

}