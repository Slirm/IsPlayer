package com.example.isplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.isplayer.domain.model.LocalVideo

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val folderId: Long = 1,
    val uri: String,
    val title: String,
    val duration: Long,
    val size: Long,
    val dateAdded: Long,
    val width: Int,
    val height: Int
) {
    fun toDomain(): LocalVideo {
        return LocalVideo(
            id = id,
            folderId = folderId,
            uri = uri,
            title = title,
            duration = duration,
            size = size,
            dateAdded = dateAdded,
            width = width,
            height = height
        )
    }
}

fun LocalVideo.toEntity(): VideoEntity {
    return VideoEntity(
        id = id,
        folderId = folderId,
        uri = uri,
        title = title,
        duration = duration,
        size = size,
        dateAdded = dateAdded,
        width = width,
        height = height
    )
}
