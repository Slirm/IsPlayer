package com.example.isplayer.presentation.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isplayer.domain.model.LocalVideo
import com.example.isplayer.domain.usecase.GetVideosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getVideosUseCase: GetVideosUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _playlist = MutableStateFlow<List<LocalVideo>>(emptyList())
    val playlist: StateFlow<List<LocalVideo>> = _playlist.asStateFlow()

    init {
        val folderId = savedStateHandle.get<String>("folderId")?.toLongOrNull() ?: 1L
        getVideosUseCase(folderId)
            .onEach { videos ->
                _playlist.value = videos
            }
            .catch { e ->
                // Handle error
            }
            .launchIn(viewModelScope)
    }
}
