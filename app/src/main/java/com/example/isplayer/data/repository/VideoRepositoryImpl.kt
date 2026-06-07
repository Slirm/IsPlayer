package com.example.isplayer.data.repository

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.isplayer.data.local.dao.FolderDao
import com.example.isplayer.data.local.dao.VideoDao
import com.example.isplayer.data.local.entity.FolderEntity
import com.example.isplayer.data.local.entity.toDomain
import com.example.isplayer.data.local.entity.toEntity
import com.example.isplayer.domain.model.Folder
import com.example.isplayer.domain.model.LocalVideo
import com.example.isplayer.domain.repository.VideoRepository
import com.example.isplayer.utils.VideoMetadataUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class VideoRepositoryImpl(
    private val videoDao: VideoDao,
    private val folderDao: FolderDao,
    private val context: Context
) : VideoRepository {

    override fun getAllVideos(): Flow<List<LocalVideo>> {
        return videoDao.getAllVideos().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getVideosByFolder(folderId: Long): Flow<List<LocalVideo>> {
        return videoDao.getVideosByFolder(folderId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addVideo(video: LocalVideo) {
        withContext(Dispatchers.IO) {
            videoDao.insertVideo(video.toEntity())
        }
    }

    override suspend fun removeVideo(video: LocalVideo) {
        withContext(Dispatchers.IO) {
            videoDao.deleteVideo(video.toEntity())
        }
    }

    override suspend fun getVideoByUri(uri: String): LocalVideo? {
        return withContext(Dispatchers.IO) {
            videoDao.getVideoByUri(uri)?.toDomain()
        }
    }

    override fun getAllFolders(): Flow<List<Folder>> {
        return folderDao.getAllFolders().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addFolder(name: String): Long {
        return withContext(Dispatchers.IO) {
            val existing = folderDao.getFolderByName(name)
            if (existing != null) {
                existing.id
            } else {
                folderDao.insertFolder(FolderEntity(name = name))
            }
        }
    }

    override suspend fun getDefaultFolder(): Folder? {
        return withContext(Dispatchers.IO) {
            folderDao.getDefaultFolder()?.toDomain()
        }
    }

    //扫描默认文件夹
    override suspend fun scanDefaultFolder() {
        withContext(Dispatchers.IO) {
            // 确保首页（默认文件夹）存在
            var defaultFolder = folderDao.getDefaultFolder()
            if (defaultFolder == null) {
                val id = folderDao.insertFolder(FolderEntity(name = "首页", isDefault = true))
                defaultFolder = folderDao.getFolderByName("首页")
            }
            val defaultFolderId = defaultFolder?.id ?: 1L

            // 我们以系统标准的 "Movies" 文件夹和 "DCIM" (相机拍摄视频) 为默认加载目录
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATA
            )
            
            // Limit to Movies and DCIM folder
            val selection = "${MediaStore.Video.Media.DATA} LIKE ? OR ${MediaStore.Video.Media.DATA} LIKE ?"
            val selectionArgs = arrayOf("%Movies%", "%DCIM%")

            var count = 0
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                
                while (cursor.moveToNext()) {
                    count++
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val size = cursor.getLong(sizeColumn)
                    val duration = cursor.getLong(durationColumn)
                    val contentUri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())

                    // check if exists
                    if (videoDao.getVideoByUri(contentUri.toString()) == null) {
                        // 使用 MediaStore 提供的 duration，避免每个文件都用 MediaMetadataRetriever 去解析，这样速度会快很多
                        val video = VideoMetadataUtils.extractVideoMetadata(context, contentUri, size, name)
                        // 如果 MediaMetadataRetriever 失败或者没取到，使用 MediaStore 的 fallback
                        val finalDuration = if (video.duration > 0) video.duration else duration
                        
                        videoDao.insertVideo(video.copy(folderId = defaultFolderId, duration = finalDuration).toEntity())
                    }
                }
            }

            // 如果手机里真的一部视频都没有，我们自动扫描 assets 文件夹下的所有视频并加载进来
            if (count == 0) {
                try {
                    val assetsFiles = context.assets.list("") ?: emptyArray()
                    val videoExtensions = listOf("mp4", "mkv", "avi", "webm", "ts")
                    
                    for (fileName in assetsFiles) {
                        val ext = fileName.substringAfterLast('.', "").lowercase()
                        if (videoExtensions.contains(ext)) {
                            val assetUri = "file:///android_asset/$fileName"
                            
                            if (videoDao.getVideoByUri(assetUri) == null) {
                                // For asset files, we need to extract metadata
                                val uri = Uri.parse(assetUri)
                                val video = VideoMetadataUtils.extractVideoMetadata(context, uri, 0, fileName)
                                
                                videoDao.insertVideo(
                                    video.copy(
                                        folderId = defaultFolderId,
                                        title = fileName
                                    ).toEntity()
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
