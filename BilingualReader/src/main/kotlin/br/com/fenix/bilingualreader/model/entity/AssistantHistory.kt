package br.com.fenix.bilingualreader.model.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.fenix.bilingualreader.model.enums.AssistantMessageRole
import br.com.fenix.bilingualreader.model.enums.Type
import br.com.fenix.bilingualreader.util.constants.DataBaseConsts
import java.time.LocalDateTime
import br.com.fenix.bilingualreader.model.interfaces.Entity as EntityBase

@Entity(
    tableName = DataBaseConsts.ASSISTANT_HISTORY.TABLE_NAME,
    indices = [Index(value = [DataBaseConsts.ASSISTANT_HISTORY.COLUMNS.FK_ID_REFERENCE, DataBaseConsts.ASSISTANT_HISTORY.COLUMNS.TYPE])]
)
data class AssistantHistory(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DataBaseConsts.ASSISTANT_HISTORY.COLUMNS.ID)
    override var id: Long?,

    @ColumnInfo(name = DataBaseConsts.ASSISTANT_HISTORY.COLUMNS.FK_ID_REFERENCE)
    val fkReference: Long,

    @ColumnInfo(name = DataBaseConsts.ASSISTANT_HISTORY.COLUMNS.TYPE)
    val type: Type,

    @ColumnInfo(name = DataBaseConsts.ASSISTANT_HISTORY.COLUMNS.ROLE)
    val role: AssistantMessageRole,

    @ColumnInfo(name = DataBaseConsts.ASSISTANT_HISTORY.COLUMNS.MESSAGE)
    val message: String,

    @ColumnInfo(name = DataBaseConsts.ASSISTANT_HISTORY.COLUMNS.DATE)
    val date: LocalDateTime
) : EntityBase<Long, AssistantHistory> {

    @Ignore
    constructor(
        fkReference: Long,
        type: Type,
        role: AssistantMessageRole,
        message: String
    ) : this(null, fkReference, type, role, message, LocalDateTime.now())
}
