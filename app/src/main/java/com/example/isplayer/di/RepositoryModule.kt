package com.example.isplayer.di

import android.app.Application
import com.example.isplayer.data.local.dao.FolderDao
import com.example.isplayer.data.local.dao.VideoDao
import com.example.isplayer.data.repository.VideoRepositoryImpl
import com.example.isplayer.domain.repository.VideoRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideVideoRepository(
        videoDao: VideoDao,
        folderDao: FolderDao,
        app: Application
    ): VideoRepository {
        return VideoRepositoryImpl(videoDao, folderDao, app)
    }
}
