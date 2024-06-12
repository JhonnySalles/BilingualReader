package br.com.fenix.bilingualreader.model.interfaces

import br.com.fenix.bilingualreader.model.entity.Library
import br.com.fenix.bilingualreader.model.enums.FileType
import br.com.fenix.bilingualreader.model.enums.Type
import java.time.LocalDateTime

interface History {
    val id: Long?
    val fkLibrary: Long?
    val name: String
    var title: String
    var bookMark: Int
    var pages: Int
    var favorite: Boolean
    var excluded: Boolean
    var fileSize: Long
    var lastAccess: LocalDateTime?
    var library: Library
    val fileType: FileType
    val type: Type
}