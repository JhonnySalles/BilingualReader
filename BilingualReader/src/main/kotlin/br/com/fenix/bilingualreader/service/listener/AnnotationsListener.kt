package br.com.fenix.bilingualreader.service.listener

import android.view.View
import br.com.fenix.bilingualreader.model.interfaces.Annotation

interface AnnotationsListener {
    fun onClick(annotation: Annotation)
    fun onClickFavorite(annotation: Annotation)
    fun onClickOptions(annotation: Annotation, view: View, position: Int)
    fun onClickNote(annotation: Annotation, position: Int)
}