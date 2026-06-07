package com.example.isplayer.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.isplayer.data.local.dao.FolderDao
import com.example.isplayer.data.local.dao.VideoDao
import com.example.isplayer.data.local.entity.FolderEntity
import com.example.isplayer.data.local.entity.VideoEntity

@Database(entities = [VideoEntity::class, FolderEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val videoDao: VideoDao
    abstract val folderDao: FolderDao

    companion object {
        const val DATABASE_NAME = "cinematic_db"
    }
}
