package com.example.isplayer.domain.usecase

import com.example.isplayer.domain.model.Folder
import com.example.isplayer.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFoldersUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    operator fun invoke(): Flow<List<Folder>> {
        return repository.getAllFolders()
    }
}
