package br.com.fenix.bilingualreader.service.listener

import android.view.View
import br.com.fenix.bilingualreader.model.entity.Vocabulary

interface VocabularyCardListener {
    fun onClick(vocabulary: Vocabulary)
    fun onClickLong(vocabulary: Vocabulary, view: View, position: Int)
    fun onClickFavorite(vocabulary: Vocabulary)
}