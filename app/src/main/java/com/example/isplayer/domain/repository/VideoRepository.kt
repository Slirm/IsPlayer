package com.example.isplayer.domain.repository

import com.example.isplayer.domain.model.Folder
import com.example.isplayer.domain.model.LocalVideo
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    fun getAllVideos(): Flow<List<LocalVideo>>
    fun getVideosByFolder(folderId: Long): Flow<List<LocalVideo>>
    suspend fun addVideo(video: LocalVideo)
    suspend fun removeVideo(video: LocalVideo)
    suspend fun scanDefaultFolder()
    suspend fun getVideoByUri(uri: String): LocalVideo?
    
    fun getAllFolders(): Flow<List<Folder>>
    suspend fun addFolder(name: String): Long
    suspend fun getDefaultFolder(): Folder?
}
