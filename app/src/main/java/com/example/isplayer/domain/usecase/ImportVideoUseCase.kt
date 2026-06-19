package com.example.isplayer.domain.usecase

import com.example.isplayer.domain.model.LocalVideo
import com.example.isplayer.domain.repository.VideoRepository
import javax.inject.Inject

class ImportVideoUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    suspend operator fun invoke(video: LocalVideo) {
        val existingVideo = repository.getVideoByUri(video.uri)
        if (existingVideo == null) {
            repository.addVideo(video)
        }
    }

    suspend fun exists(uri: String): Boolean {
        return repository.getVideoByUri(uri) != null
    }
}
