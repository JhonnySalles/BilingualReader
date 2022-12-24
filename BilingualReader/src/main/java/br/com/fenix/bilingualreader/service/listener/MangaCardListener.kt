package br.com.fenix.bilingualreader.service.listener

import android.view.View
import br.com.fenix.bilingualreader.model.entity.Manga

interface MangaCardListener {
    fun onClick(manga: Manga)
    fun onClickLong(manga: Manga, view : View, position: Int)
}