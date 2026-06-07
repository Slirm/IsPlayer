package com.example.isplayer.domain.usecase

import com.example.isplayer.domain.model.LocalVideo
import com.example.isplayer.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetVideosUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    operator fun invoke(folderId: Long): Flow<List<LocalVideo>> {
        return repository.getVideosByFolder(folderId)
    }
}
