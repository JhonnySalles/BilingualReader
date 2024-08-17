package br.com.fenix.bilingualreader.service.listener

import android.view.View
import br.com.fenix.bilingualreader.model.entity.Manga

interface MangaCardListener : BaseCardListener {
    fun onClick(manga: Manga)
    fun onClickFavorite(manga: Manga)
    fun onClickConfig(manga: Manga, root: View, item: View, position: Int)
    fun onClickLong(manga: Manga, view : View, position: Int)
}