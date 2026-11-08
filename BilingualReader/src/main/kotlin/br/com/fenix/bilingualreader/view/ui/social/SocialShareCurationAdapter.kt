package br.com.fenix.bilingualreader.view.ui.social

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.databinding.LineCardSocialShareCurationBinding
import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.HistoryStatistics
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.service.controller.BookImageCoverController
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController

class SocialShareCurationAdapter(
    private val onToggleItem: (String, Boolean) -> Unit
) : RecyclerView.Adapter<SocialShareCurationAdapter.ViewHolder>() {

    private var mItems = listOf<HistoryStatistics>()
    private val mExcludedKeys = mutableSetOf<String>()

    init {
        setHasStableIds(true)
    }

    fun updateData(items: List<HistoryStatistics>, excludedKeys: Set<String>) {
        mItems = items
        mExcludedKeys.clear()
        mExcludedKeys.addAll(excludedKeys)
        notifyDataSetChanged()
    }

    fun updateExcludedKeys(excludedKeys: Set<String>) {
        if (mExcludedKeys != excludedKeys) {
            mExcludedKeys.clear()
            mExcludedKeys.addAll(excludedKeys)
            notifyDataSetChanged()
        }
    }

    override fun getItemId(position: Int): Long {
        if (position !in mItems.indices) return RecyclerView.NO_ID
        val base = mItems[position].base
        val id = base.id ?: position.toLong()
        return if (base.type == Type.MANGA) id else -id - 1L
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LineCardSocialShareCurationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(mItems[position])
    }

    override fun getItemCount(): Int = mItems.size

    inner class ViewHolder(private val binding: LineCardSocialShareCurationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistoryStatistics) {
            val base = item.base
            val key = "${base.type}_${base.id}"
            val isChecked = !mExcludedKeys.contains(key)

            binding.socialCurationTitle.text = if (base.title.isNotBlank()) base.title else base.name

            binding.socialCurationCheckbox.setOnCheckedChangeListener(null)
            binding.socialCurationCheckbox.isChecked = isChecked

            val toggleAction: (Boolean) -> Unit = { checked ->
                if (checked) {
                    mExcludedKeys.remove(key)
                } else {
                    mExcludedKeys.add(key)
                }
                onToggleItem(key, checked)
            }

            binding.socialCurationCheckbox.setOnCheckedChangeListener { _, checked ->
                toggleAction(checked)
            }

            binding.root.setOnClickListener {
                val newChecked = !binding.socialCurationCheckbox.isChecked
                binding.socialCurationCheckbox.isChecked = newChecked
            }

            val context = binding.root.context
            if (base is Manga) {
                MangaImageCoverController.instance.setImageCoverAsync(context, base, binding.socialCurationCover, null, isCoverSize = true)
            } else if (base is Book) {
                BookImageCoverController.instance.setImageCoverAsync(context, base, binding.socialCurationCover, null, isCoverSize = true)
            }
        }
    }
}

