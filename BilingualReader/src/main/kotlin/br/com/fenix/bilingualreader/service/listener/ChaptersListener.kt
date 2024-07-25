package br.com.fenix.bilingualreader.service.listener

import br.com.fenix.bilingualreader.model.entity.Chapters

interface ChapterCardListener {
    fun onClick(page: Chapters)
    fun onLongClick(page: Chapters)
}

interface ChapterLoadListener {
    fun onLoading(page: Int)
}