package com.example.isplayer.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.isplayer.domain.model.LocalVideo

object VideoMetadataUtils {
    fun extractVideoMetadata(context: Context, uri: Uri, size: Long = 0, title: String = "Unknown"): LocalVideo {
        val retriever = MediaMetadataRetriever()
        var duration = 0L
        var width = 0
        var height = 0
        try {
            // For SAF URIs (content://...), MediaMetadataRetriever needs a FileDescriptor to work reliably
            if (uri.scheme == "content") {
                try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        retriever.setDataSource(pfd.fileDescriptor)
                    }
                } catch (e: Exception) {
                    // Fallback to setting uri directly if file descriptor fails
                    retriever.setDataSource(context, uri)
                }
            } else {
                retriever.setDataSource(context, uri)
            }
            
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            
            duration = durationStr?.toLongOrNull() ?: 0L
            width = widthStr?.toIntOrNull() ?: 0
            height = heightStr?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            android.util.Log.e("VideoMetadataUtils", "Failed to extract metadata for uri: $uri", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }

        return LocalVideo(
            uri = uri.toString(),
            title = title,
            duration = duration,
            size = size,
            dateAdded = System.currentTimeMillis(),
            width = width,
            height = height
        )
    }

    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun formatSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(sizeBytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", sizeBytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
