package br.com.fenix.bilingualreader.service.listener

import br.com.fenix.bilingualreader.model.entity.Pages

interface ChapterCardListener {
    fun onClick(page: Pages)
}