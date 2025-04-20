package br.com.fenix.bilingualreader.model.interfaces

import br.com.fenix.bilingualreader.model.enums.MarkType
import br.com.fenix.bilingualreader.model.enums.Type

interface Annotation {
    val id_parent: Long
    val chapterNumber: Float
    val chapter: String
    var annotation: String
    val markType: MarkType
    var parent: Annotation?
    var isRoot: Boolean
    var isTitle: Boolean
    var count: Int
    val type: Type
}