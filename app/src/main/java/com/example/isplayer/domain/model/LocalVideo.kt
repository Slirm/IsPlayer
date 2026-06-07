package com.example.isplayer.domain.model

data class LocalVideo(
    val id: Long = 0,
    val folderId: Long = 1,
    val uri: String,
    val title: String,
    val duration: Long,
    val size: Long,
    val dateAdded: Long,
    val width: Int = 0,
    val height: Int = 0
) {
    val is4K: Boolean
        get() = width >= 3840 || height >= 2160
}
