package br.com.fenix.bilingualreader.service.listener

import br.com.fenix.bilingualreader.model.enums.Color
import br.com.fenix.bilingualreader.model.enums.Filter

interface AnnotationListener {
    fun getFilters() : Set<Filter>
    fun filterType(filter: Filter, isRemove: Boolean = false)

    fun getColors() : Set<Color>

    fun filterColor(color: Color, isRemove: Boolean = false)

    fun getChapters() : Map<String, Float>

    fun filterChapter(chapter: String, isRemove: Boolean = false)

}