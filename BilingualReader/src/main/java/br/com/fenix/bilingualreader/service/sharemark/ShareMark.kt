package br.com.fenix.bilingualreader.service.sharemark

import br.com.fenix.bilingualreader.model.entity.Book
import br.com.fenix.bilingualreader.model.entity.Manga
import br.com.fenix.bilingualreader.model.enums.ShareMarkType


interface ShareMark {
    fun clearLastSync()

    /**
     * @param update  Function call when object is updated
     * @param ending  Function call when process finish, parameter make a process results
     *
     */
    fun mangaShareMark(update: (manga: Manga) -> (Unit), ending: (processed: ShareMarkType) -> (Unit))

    /**
     * @param update  Function call when object is updated
     * @param ending  Function call when process finish, parameter is true if can processed list
     *
     */
    fun bookShareMark(update: (book: Book) -> (Unit), ending: (processed: ShareMarkType) -> (Unit))

}