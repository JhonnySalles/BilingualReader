package br.com.fenix.bilingualreader.view.adapter.library

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.LibraryMangaType
import br.com.fenix.bilingualreader.service.controller.MangaImageCoverController
import br.com.fenix.bilingualreader.service.listener.MangaCardListener
import br.com.fenix.bilingualreader.util.constants.GeneralConsts
import br.com.fenix.bilingualreader.util.helpers.AdapterUtil.AdapterUtils
import br.com.fenix.bilingualreader.util.helpers.Util
import br.com.fenix.bilingualreader.view.components.TextViewWithBorder
import com.google.android.material.card.MaterialCardView


class MangaSeparatorGridViewHolder(var type: LibraryMangaType, itemView: View, private val listener: MangaCardListener) : RecyclerView.ViewHolder(itemView) {

    companion object {
        var mMangaImage: Int = 0
        lateinit var mDefaultImageCover1: Bitmap
        lateinit var mDefaultImageCover2: Bitmap
        lateinit var mDefaultImageCover3: Bitmap
        lateinit var mDefaultImageCover4: Bitmap
        lateinit var mDefaultImageCover5: Bitmap
        private var mDefaultsLoaded = false

        private fun ensureDefaults(itemView: View) {
            if (mDefaultsLoaded) return
            mMangaImage = itemView.resources.getDimension(R.dimen.manga_separator_grid_card_image).toInt()
            mDefaultImageCover1 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_1)
            mDefaultImageCover2 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_2)
            mDefaultImageCover3 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_3)
            mDefaultImageCover4 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_4)
            mDefaultImageCover5 = BitmapFactory.decodeResource(itemView.resources, R.mipmap.book_cover_5)
            mDefaultsLoaded = true
        }
    }

    private val mangaImage: ImageView = itemView.findViewById(R.id.manga_grid_image_cover)
    private val mangaTitle: TextViewWithBorder = itemView.findViewById(R.id.manga_grid_text_title)
    private val mangaFileType: TextViewWithBorder = itemView.findViewById(R.id.manga_grid_file_type)
    private val mangaPagesRead: TextViewWithBorder = itemView.findViewById(R.id.manga_grid_pages)
    private val mangaLastAccess: TextViewWithBorder = itemView.findViewById(R.id.manga_grid_last_access)
    private val mangaProgress: ProgressBar = itemView.findViewById(R.id.manga_grid_progress)
    private val cardView: MaterialCardView = itemView.findViewById(R.id.manga_grid_card)
    private val favorite: LinearLayout = itemView.findViewById(R.id.manga_grid_favorite)
    private val favoriteIcon: ImageView = itemView.findViewById(R.id.manga_grid_favorite_icon)
    private val config: LinearLayout = itemView.findViewById(R.id.manga_grid_config)
    private val configIcon: ImageView = itemView.findViewById(R.id.manga_grid_config_icon)
    private val subtitle: ImageView = itemView.findViewById(R.id.manga_grid_has_subtitle)

    init {
        ensureDefaults(itemView)
    }

    @SuppressLint("SetTextI18n")
    fun bind(manga: Manga) {
        if (manga.favorite)
            favorite.visibility = View.VISIBLE
        else
            favorite.visibility = View.GONE

        subtitle.visibility = if (manga.hasSubtitle) {
            if (manga.lastVocabImport != null)
                subtitle.setImageResource(R.drawable.ico_subtitles_imported)
            else
                subtitle.setImageResource(R.drawable.ico_subtitles_exist)
            View.VISIBLE
        } else View.GONE

        favorite.setOnClickListener {
            manga.favorite = !manga.favorite
            favoriteIcon.setImageResource(if (manga.favorite) R.drawable.ico_favorite_mark else R.drawable.ico_favorite_unmark)
            listener.onClickFavorite(manga)
        }

        favoriteIcon.setImageResource(if (manga.favorite) R.drawable.ico_favorite_mark else R.drawable.ico_favorite_unmark)
        config.setOnClickListener { listener.onClickConfig(manga, cardView, configIcon, layoutPosition) }

        val isLandscape = itemView.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val cardSize = AdapterUtils.getMangaCardSize(itemView.context, type, isLandscape)
        cardView.layoutParams.width = cardSize.first
        cardView.layoutParams.height = cardSize.second
        mangaImage.layoutParams.height = mMangaImage
        cardView.setOnClickListener { listener.onClick(manga, itemView) }
        cardView.setOnLongClickListener {
            listener.onClickLong(manga, itemView, layoutPosition)
            true
        }

        val image = when ((1..5).random()) {
            1 -> mDefaultImageCover1
            2 -> mDefaultImageCover2
            3 -> mDefaultImageCover3
            4 -> mDefaultImageCover4
            else -> mDefaultImageCover5
        }

        MangaImageCoverController.instance.setImageCoverAsync(itemView.context, manga, mangaImage, image)

        val isSmall = manga.lastAccess != null && manga.bookMark > 0 && type != LibraryMangaType.GRID_BIG && type != LibraryMangaType.SEPARATOR_BIG

        mangaTitle.text = manga.title
        mangaFileType.text = Util.getExtensionFromPath(manga.path).uppercase()
        mangaLastAccess.text = if (manga.lastAccess != null) GeneralConsts.formatterDate(itemView.context, manga.lastAccess!!, isSmall) else ""
        mangaPagesRead.text = if (isSmall) {
            val percent: Float = (manga.bookMark.toFloat() / manga.pages) * 100
            "${Util.formatDecimal(percent)} %"
        } else
            "${manga.bookMark} / ${manga.pages}"

        mangaProgress.max = manga.pages
        mangaProgress.setProgress(manga.bookMark, false)
    }

}
