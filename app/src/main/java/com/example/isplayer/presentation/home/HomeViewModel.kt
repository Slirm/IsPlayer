package com.example.isplayer.presentation.home

import android.content.Context
import android.net.Uri
import android.util.Log
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Get video metadata and import it
                val fileInfo = getFileInfo(uri)
                Log.d("VideoImport", "Single file imported: uri=$uri, name=${fileInfo.first}, size=${fileInfo.second}")
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
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                for (uri in sourceUris) {
                    val documentTree = DocumentFile.fromTreeUri(context, uri)
                    if (documentTree != null) {
                        scanDocumentTree(documentTree, targetFolderId)
                    }
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

    private suspend fun scanDocumentTree(directory: DocumentFile, targetFolderId: Long) = coroutineScope {
        // 移除了 .m3u8 和 .ts，因为本地 m3u8（尤其是浏览器下载的）通常包含加密或路径权限问题，无法直接播放
        val videoExtensions = listOf(".mp4", ".mkv", ".avi", ".mov", ".flv", ".wmv", ".webm")
        
        val deferredJobs = mutableListOf<kotlinx.coroutines.Deferred<Unit>>()
        // 限制并发数量，避免 MediaMetadataRetriever 过多导致内存溢出或底层崩溃
        val semaphore = Semaphore(5)

        suspend fun scanRecursively(dir: DocumentFile) {
            val files = dir.listFiles()
            
            // 为了提升性能，我们不再根据 m3u8 直接跳过整个文件夹（因为 MP4 可能和它们混在一起）
            // 但是我们要避免在几千个 .ts 文件上浪费时间去评估复杂的过滤逻辑。
            files.forEach { file ->
                if (file.isDirectory) {
                    // 递归扫描子文件夹
                    scanRecursively(file)
                } else if (file.isFile) {
                    val name = file.name?.lowercase() ?: ""
                    
                    // 1. 最外层极速拦截：如果是隐藏文件夹（以 . 开头）或隐藏文件，但如果是我们要的视频格式则放行
                    val isHidden = name.startsWith(".")
                    val isVideoByExt = videoExtensions.any { ext -> name.endsWith(ext) }
                    
                    if (isHidden && !isVideoByExt) {
                        return@forEach // 如果是隐藏文件且不是我们支持的视频格式，直接跳过
                    }
                    
                    // 2. 过滤已知的流媒体碎片
                    if (name.endsWith(".ts") || name.endsWith(".m3u8") || name.endsWith(".key")) {
                        return@forEach // 等同于 continue，直接进入下一个文件
                    }
                    
                    val isVideoByType = file.type?.startsWith("video/") == true && file.type != "video/mp2ts"
                    
                    if (isVideoByType || isVideoByExt) {
                        // 只有通过了极速拦截，且确实是我们要的视频格式（如 .mp4），我们再去查它的大小
                        // 因为 file.length() 在 SAF 下是一个耗时的跨进程 I/O 操作
                        val isTooSmall = file.length() < 1024 * 1024 // 必须大于 1MB
                        
                        if (!isTooSmall) {
                            deferredJobs.add(async {
                                semaphore.acquire() // Use acquire() and try-finally instead of withPermit to ensure proper release
                                try {
                                    val documentUri = file.uri
                                    val uriStr = documentUri.toString()
                                    
                                    // 1. 先查数据库，如果已经存在，直接跳过，避免耗时的元数据提取
                                    val exists = importVideoUseCase.exists(uriStr)
                                    
                                    if (!exists) {
                                        // 2. 提取元数据 (最耗时的一步，现在在并发和子线程中执行)
                                        val video = VideoMetadataUtils.extractVideoMetadata(
                                            context,
                                            documentUri,
                                            file.length(),
                                            file.name ?: "Unknown Video"
                                        )
                                        
                                        // SAF 权限保留：导入时尝试保留对这个具体文件的读取权限
                                        try {
                                            context.contentResolver.takePersistableUriPermission(
                                                documentUri,
                                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            )
                                        } catch (e: Exception) {
                                            // 忽略无法获取持久化权限的文件
                                        }
                                        
                                        val videoWithFolder = video.copy(folderId = targetFolderId)
                                        // 3. 存入数据库
                                        importVideoUseCase(videoWithFolder)
                                    }
                                } catch (e: Exception) {
                                    // 提取失败时忽略该文件
                                } finally {
                                    semaphore.release()
                                }
                                Unit // Ensure the block returns Unit
                            })
                        }
                    }
                }
            }
        }
        
        scanRecursively(directory)
        // 等待所有提取和插入任务完成
        deferredJobs.awaitAll()
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
