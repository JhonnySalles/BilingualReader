package br.com.fenix.bilingualreader.view.adapter.vocabulary

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.entity.VocabularyManga
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import com.google.android.material.card.MaterialCardView

class VocabularyMangaListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    companion object {
        lateinit var mDefaultImageCover1: Bitmap
        lateinit var mDefaultImageCover2: Bitmap
        lateinit var mDefaultImageCover3: Bitmap
        lateinit var mDefaultImageCover4: Bitmap
        lateinit var mDefaultImageCover5: Bitmap
    }

    init {
        mDefaultImageCover1 =
            BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_1)
        mDefaultImageCover2 =
            BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_2)
        mDefaultImageCover3 =
            BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_3)
        mDefaultImageCover4 =
            BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_4)
        mDefaultImageCover5 =
            BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_5)
    }

    fun bind(vocabulary: VocabularyManga, mangaList: MutableMap<Long, Bitmap?>) {
        val appear = itemView.findViewById<TextView>(R.id.vocabulary_manga_list_appear)
        val card = itemView.findViewById<MaterialCardView>(R.id.vocabulary_manga_list_image_card)
        val cover = itemView.findViewById<ImageView>(R.id.vocabulary_manga_list_image_cover)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            TooltipCompat.setTooltipText(card, vocabulary.manga?.name ?: "")
        else
            card.tooltipText = vocabulary.manga?.name ?: ""

        appear.text = vocabulary.appears.toString()

        val image = when ((1..5).random()) {
            1 -> mDefaultImageCover1
            2 -> mDefaultImageCover2
            3 -> mDefaultImageCover3
            4 -> mDefaultImageCover4
            else -> mDefaultImageCover5
        }

        if (mangaList.contains(vocabulary.idManga)) {
            mangaList[vocabulary.idManga]?.let {
                cover.setImageBitmap(it)
            }
        } else {
            cover.setImageBitmap(null)
            setCover(cover, image, vocabulary.manga!!, mangaList)
        }
    }

    private fun setCover(cover: ImageView, notLocate: Bitmap, manga: Manga, mangaList: MutableMap<Long, Bitmap?>) {
        MangaImageCoverController.instance.setImageCoverAsync(itemView.context, manga, cover, null, true) { b ->
            if (b != null) {
                mangaList[manga.id!!] = b
                // Limit 2k in size
                if (mangaList.size > 2000)
                    mangaList.remove(mangaList.entries.first().key)
            } else
                cover.setImageBitmap(notLocate)
        }
    }


}