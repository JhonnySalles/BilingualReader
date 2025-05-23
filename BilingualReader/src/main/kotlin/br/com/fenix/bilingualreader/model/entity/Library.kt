package br.com.fenix.bilingualreader.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fenix.bilingualreader.model.enums.Libraries
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import java.io.File
import java.io.Serializable


@Entity(
    tableName = DataBaseConsts.LIBRARIES.TABLE_NAME,
    indices = [Index(value = [DataBaseConsts.LIBRARIES.COLUMNS.TITLE])]
)
data class Library(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.LIBRARIES.COLUMNS.ID)
    var id: Long?,

    @ColumnInfo(name = DataBaseConsts.LIBRARIES.COLUMNS.TITLE)
    var title: String = Libraries.DEFAULT.name,

    @ColumnInfo(name = DataBaseConsts.LIBRARIES.COLUMNS.PATH)
    var path: String = Libraries.DEFAULT.name,

    @ColumnInfo(name = DataBaseConsts.LIBRARIES.COLUMNS.LANGUAGE)
    var language: Libraries = Libraries.DEFAULT,

    @ColumnInfo(name = DataBaseConsts.LIBRARIES.COLUMNS.TYPE)
    var type: Type = Type.MANGA,

    @ColumnInfo(name = DataBaseConsts.LIBRARIES.COLUMNS.ENABLED)
    var enabled: Boolean = true,

    @ColumnInfo(name = DataBaseConsts.LIBRARIES.COLUMNS.EXCLUDED)
    var excluded: Boolean = false
) : Serializable {

    @Ignore
    var menuKey: Int = 0

    @Ignore
    var file: File = File(path)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Library

        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }

    fun merge(library: Library) : Long? {
        this.title = library.title
        this.path = library.path
        this.language = library.language
        this.type = library.type
        this.enabled = library.enabled

        return this.id
    }
}

