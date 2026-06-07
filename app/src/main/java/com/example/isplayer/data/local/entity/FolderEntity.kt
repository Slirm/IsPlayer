package com.example.isplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.isplayer.domain.model.Folder

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false
)

fun FolderEntity.toDomain(): Folder {
    return Folder(
        id = id,
        name = name,
        isDefault = isDefault
    )
}

fun Folder.toEntity(): FolderEntity {
    return FolderEntity(
        id = id,
        name = name,
        isDefault = isDefault
    )
}
