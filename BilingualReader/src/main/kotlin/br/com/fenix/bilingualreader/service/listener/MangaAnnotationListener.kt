package br.com.fenix.bilingualreader.service.listener

import br.com.fenix.bilingualreader.model.entity.MangaAnnotation

interface MangaAnnotationListener {
    fun onClick(annotation: MangaAnnotation)
}