package br.com.fenix.bilingualreader.view.ui.social

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.databinding.ListItemSocialSharePreviewBinding
import kotlin.math.roundToInt

class SocialSharePreviewAdapter(
    private val onClickBitmap: (Bitmap) -> Unit
) : RecyclerView.Adapter<SocialSharePreviewAdapter.ViewHolder>() {

    private var mBitmaps = listOf<Bitmap>()

    fun updateBitmaps(bitmaps: List<Bitmap>) {
        mBitmaps = bitmaps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemSocialSharePreviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(mBitmaps[position], mBitmaps.size)
    }

    override fun getItemCount(): Int = mBitmaps.size

    inner class ViewHolder(
        private val binding: ListItemSocialSharePreviewBinding,
        private val parent: ViewGroup
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bitmap: Bitmap, totalCount: Int) {
            val lp = (binding.socialPreviewItemCard.layoutParams as? ViewGroup.MarginLayoutParams)
                ?: ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val parentW = parent.measuredWidth.takeIf { it > 0 }
                ?: parent.resources.displayMetrics.widthPixels

            if (totalCount > 1) {
                lp.width = (parentW * 0.86f).roundToInt()
                lp.marginEnd = (12 * parent.resources.displayMetrics.density).roundToInt()
            } else {
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                lp.marginEnd = 0
            }
            binding.socialPreviewItemCard.layoutParams = lp

            binding.socialPreviewItemImage.setImageBitmap(bitmap)
            binding.socialPreviewItemCard.setOnClickListener {
                onClickBitmap(bitmap)
            }
        }
    }
}
