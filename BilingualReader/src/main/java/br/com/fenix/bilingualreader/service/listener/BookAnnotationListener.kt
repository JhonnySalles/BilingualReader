package br.com.fenix.bilingualreader.service.listener

import android.view.View
import br.com.fenix.bilingualreader.model.entity.BookAnnotation

interface BookAnnotationListener {
    fun onClick(annotation: BookAnnotation)
    fun onClickFavorite(annotation: BookAnnotation)
    fun onClickOptions(annotation: BookAnnotation, view: View, position: Int)
    fun onClickNote(annotation: BookAnnotation)
}