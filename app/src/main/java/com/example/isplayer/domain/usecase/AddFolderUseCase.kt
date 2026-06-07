package com.example.isplayer.domain.usecase

import com.example.isplayer.domain.repository.VideoRepository
import javax.inject.Inject

class AddFolderUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    suspend operator fun invoke(name: String): Long {
        return repository.addFolder(name)
    }
}
