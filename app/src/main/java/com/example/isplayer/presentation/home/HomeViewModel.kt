package com.example.isplayer.presentation.home

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.isplayer.domain.model.Folder
import com.example.isplayer.domain.model.LocalVideo
import com.example.isplayer.domain.usecase.AddFolderUseCase
import com.example.isplayer.domain.usecase.GetFoldersUseCase
import com.example.isplayer.domain.usecase.GetVideosUseCase
import com.example.isplayer.domain.usecase.ImportVideoUseCase
import com.example.isplayer.domain.usecase.ScanDefaultFolderUseCase
import com.example.isplayer.domain.usecase.SyncVideosUseCase
import com.example.isplayer.utils.VideoMetadataUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOrder {
    NAME, DATE_ADDED, DURATION
}

data class HomeUiState(
    val folders: List<Folder> = emptyList(),
    val currentFolderId: Long = 1L,
    val videos: List<LocalVideo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getVideosUseCase: GetVideosUseCase,
    private val getFoldersUseCase: GetFoldersUseCase,
    private val addFolderUseCase: AddFolderUseCase,
    private val importVideoUseCase: ImportVideoUseCase,
    private val scanDefaultFolderUseCase: ScanDefaultFolderUseCase,
    private val syncVideosUseCase: SyncVideosUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _currentFolderId = MutableStateFlow(1L)
    
    private val _sortOrder = MutableStateFlow(SortOrder.DATE_ADDED)
    val sortOrder = _sortOrder.asStateFlow()

    init {
        viewModelScope.launch {
            // 确保应用第一次启动时，存在默认的"首页"文件夹
            try {
                addFolderUseCase("首页")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // 同步视频状态，移除已经被删除的文件
            try {
                syncVideosUseCase()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        observeFolders()
        observeVideos()
    }

    fun syncVideos() {
        viewModelScope.launch {
            try {
                syncVideosUseCase()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun observeFolders() {
        getFoldersUseCase()
            .onEach { folders ->
                _uiState.update { it.copy(folders = folders) }
                if (folders.isNotEmpty() && !folders.any { it.id == _currentFolderId.value }) {
                    selectFolder(folders.first().id)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeVideos() {
        combine(
            _currentFolderId.flatMapLatest { folderId -> getVideosUseCase(folderId) },
            _searchQuery,
            _sortOrder
        ) { videos, query, sortOrder ->
            var result = videos

            // Filter
            if (query.isNotBlank()) {
                result = result.filter { it.title.contains(query, ignoreCase = true) }
            }

            // Sort
            result = when (sortOrder) {
                SortOrder.NAME -> result.sortedBy { it.title }
                SortOrder.DATE_ADDED -> result.sortedByDescending { it.dateAdded }
                SortOrder.DURATION -> result.sortedByDescending { it.duration }
            }
            
            result
        }
        .onEach { filteredAndSortedVideos ->
            _uiState.update { it.copy(videos = filteredAndSortedVideos, isLoading = false) }
        }
        .catch { e ->
            _uiState.update { it.copy(error = e.message ?: "Unknown Error", isLoading = false) }
        }
        .launchIn(viewModelScope)
    }

    fun selectFolder(folderId: Long) {
        _currentFolderId.value = folderId
        _uiState.update { it.copy(currentFolderId = folderId) }
    }

    fun addNewFolder(name: String) {
        viewModelScope.launch {
            try {
                val id = addFolderUseCase(name)
                selectFolder(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSortOrderChange(order: SortOrder) {
        _sortOrder.value = order
    }

    fun scanDefaultFolder() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                scanDefaultFolderUseCase()
                _uiState.update { it.copy(isLoading = false) } // 确保扫描成功后取消加载状态
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun importVideo(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Get video metadata and import it
                val fileInfo = getFileInfo(uri)
                val video = VideoMetadataUtils.extractVideoMetadata(context, uri, fileInfo.second, fileInfo.first)
                val videoWithFolder = video.copy(folderId = _currentFolderId.value)
                importVideoUseCase(videoWithFolder)
                _uiState.update { it.copy(isLoading = false) } // 确保导入成功后取消加载状态
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun importVideosFromFolders(sourceUris: List<Uri>, targetFolderId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                for (uri in sourceUris) {
                    val documentTree = DocumentFile.fromTreeUri(context, uri)
                    documentTree?.let { scanDocumentTree(it, targetFolderId) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                // switch to target folder to see the imported videos
                selectFolder(targetFolderId)
            }
        }
    }

    private suspend fun scanDocumentTree(directory: DocumentFile, targetFolderId: Long) {
        val videoExtensions = listOf(".mp4", ".mkv", ".avi", ".mov", ".flv", ".wmv", ".webm", ".ts")
        
        directory.listFiles().forEach { file ->
            if (file.isDirectory) {
                // 递归扫描子文件夹
                scanDocumentTree(file, targetFolderId)
            } else if (file.isFile) {
                val isVideoByType = file.type?.startsWith("video/") == true
                val isVideoByExt = videoExtensions.any { ext -> file.name?.lowercase()?.endsWith(ext) == true }
                
                if (isVideoByType || isVideoByExt) {
                    val video = VideoMetadataUtils.extractVideoMetadata(
                        context,
                        file.uri,
                        file.length(),
                        file.name ?: "Unknown Video"
                    )
                    val videoWithFolder = video.copy(folderId = targetFolderId)
                    importVideoUseCase(videoWithFolder)
                }
            }
        }
    }

    private fun getFileInfo(uri: Uri): Pair<String, Long> {
        var name = "Unknown Video"
        var size = 0L
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        }
        if (name == "Unknown Video") {
            name = uri.path?.substringAfterLast('/') ?: name
        }
        return Pair(name, size)
    }
}
