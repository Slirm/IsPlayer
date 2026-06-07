package com.example.isplayer.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.isplayer.domain.repository.VideoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SyncVideosUseCase @Inject constructor(
    private val repository: VideoRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        try {
            // Get all videos once
            val videos = repository.getAllVideos().first()
            
            for (video in videos) {
                if (video.uri.startsWith("file:///android_asset/")) {
                    // Assets are always present, skip
                    continue
                }
                
                val uri = Uri.parse(video.uri)
                val exists = try {
                    if (uri.scheme == "content") {
                        try {
                            var isActuallyThere = false
                            // 1. Try to open file descriptor
                            context.contentResolver.openFileDescriptor(uri, "r")?.use { 
                                isActuallyThere = true
                            }
                            
                            // 2. Double check with DocumentFile if it's SAF
                            if (isActuallyThere && uri.authority != "media") {
                                val documentFile = DocumentFile.fromSingleUri(context, uri)
                                isActuallyThere = documentFile?.exists() == true && documentFile.length() > 0
                            }
                            isActuallyThere
                        } catch (e: Exception) {
                            false
                        }
                    } else {
                        // For file URIs
                        val file = java.io.File(uri.path ?: "")
                        file.exists() && file.length() > 0
                    }
                } catch (e: Exception) {
                    false
                }
                
                if (!exists) {
                    // Remove from database if file no longer exists
                    repository.removeVideo(video)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
