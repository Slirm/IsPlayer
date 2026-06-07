package com.example.isplayer.data.local.dao

import androidx.room.*
import com.example.isplayer.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos WHERE folderId = :folderId ORDER BY dateAdded DESC")
    fun getVideosByFolder(folderId: Long): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY dateAdded DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Delete
    suspend fun deleteVideo(video: VideoEntity)

    @Query("SELECT * FROM videos WHERE uri = :uri LIMIT 1")
    suspend fun getVideoByUri(uri: String): VideoEntity?
}
