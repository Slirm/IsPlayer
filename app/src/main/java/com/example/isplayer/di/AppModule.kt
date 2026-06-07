package com.example.isplayer.di

import android.app.Application
import androidx.room.Room
import com.example.isplayer.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import com.example.isplayer.data.local.dao.FolderDao
import com.example.isplayer.data.local.dao.VideoDao

import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideVideoDao(db: AppDatabase): VideoDao = db.videoDao

    @Provides
    @Singleton
    fun provideFolderDao(db: AppDatabase): FolderDao = db.folderDao
}
