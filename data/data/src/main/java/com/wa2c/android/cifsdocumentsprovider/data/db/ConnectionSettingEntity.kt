package com.samsung.cifs.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.samsung.cifs.common.values.StorageType
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = ConnectionSettingEntity.TABLE_NAME,
    indices = [
        Index(value = ["sort_order"]),
    ]
)
data class ConnectionSettingEntity(
    /** ID */
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    /** Name */
    @ColumnInfo(name = "name")
    val name: String,
    /** Type */
    @ColumnInfo(name = "type")
    val type: String = StorageType.default.value,
    /** URI */
    @ColumnInfo(name = "uri")
    val uri: String,
    /** Data (Encrypted) */
    @ColumnInfo(name = "data")
    val data: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    /** Modified Date */
    @ColumnInfo(name = "modified_date")
    val modifiedDate: Long,
) {
    companion object {
        /** Table name.  */
        const val TABLE_NAME = "connection_setting"

    }
}
