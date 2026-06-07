package com.example.isplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.isplayer.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY isDefault DESC, id ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("SELECT * FROM folders WHERE name = :name LIMIT 1")
    suspend fun getFolderByName(name: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultFolder(): FolderEntity?
}
