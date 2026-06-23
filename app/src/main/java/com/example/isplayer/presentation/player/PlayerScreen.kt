package com.example.isplayer.presentation.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SkipNext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.isplayer.utils.bounceClick
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    videoUri: String,
    videoTitle: String,
    folderId: Long,
    initialWidth: Int = 0,
    initialHeight: Int = 0,
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    val playlist by viewModel.playlist.collectAsState()

    // Player states
    var currentVideoUri by remember { mutableStateOf(videoUri) }
    var currentVideoTitle by remember { mutableStateOf(videoTitle) }
    
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }
    var lastSeekTime by remember { mutableStateOf(0L) }
    var isExiting by remember { mutableStateOf(false) }
    
    // New UI States
    var isScreenLocked by remember { mutableStateOf(false) }
    var isRotationLocked by remember { mutableStateOf(false) }
    var showPlaylist by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1f) }

    // Feedback states
    var volumeLevel by remember { mutableStateOf<Float?>(null) }
    var brightnessLevel by remember { mutableStateOf<Float?>(null) }
    var seekText by remember { mutableStateOf<String?>(null) }

    // System Services
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    val exoPlayer = remember {
        // Use DefaultDataSource.Factory but with a content resolver that can handle SAF uris properly
        val dataSourceFactory = DefaultDataSource.Factory(context)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        androidx.media3.exoplayer.ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
    }

    LaunchedEffect(currentVideoUri) {
        try {
            val uri = Uri.parse(currentVideoUri)
            
            // If it's a content:// URI from SAF, we must ensure we still have permission to read it
            if (currentVideoUri.startsWith("content://")) {
                try {
                    // 尝试检查是否有权限，如果没有则尝试获取
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Ignore, we might already have it or it's not persistable
                }
            }

            val mediaItem = if (currentVideoUri.startsWith("file:///android_asset/")) {
                val assetPath = currentVideoUri.removePrefix("file:///android_asset/")
                MediaItem.fromUri(Uri.parse("asset:///$assetPath"))
            } else {
                MediaItem.Builder()
                    .setUri(uri)
                    .build()
            }
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (e: Exception) {
            android.util.Log.e("PlayerScreen", "Error setting media item: ${e.message}")
        }
    }
    
    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    val performExit = {
        isExiting = true
        exoPlayer.pause()
        activity?.let {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            val window = it.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
        onNavigateBack()
    }

    BackHandler {
        performExit()
    }

    // Full screen management and Initial Orientation
    DisposableEffect(Unit) {
        if (activity != null) {
            val window = activity.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            
            // Set initial orientation based on video dimensions before ExoPlayer is ready
            if (!isRotationLocked && initialWidth > 0 && initialHeight > 0) {
                if (initialWidth > initialHeight) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                }
            }
        }
        onDispose {
            if (activity != null) {
                val window = activity.window
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    // ExoPlayer Listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                // 仅保留基本错误日志，去除详细的追踪日志
                android.util.Log.e("PlayerScreen", "ExoPlayer Error: ${error.message}")
            }

            override fun onIsPlayingChanged(isPlayingState: Boolean) {
                isPlaying = isPlayingState
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration
                }
            }
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0 && !isRotationLocked) {
                    if (videoSize.width > videoSize.height) {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    } else {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Poll current position
    LaunchedEffect(Unit) {
        while (true) {
            if (!isSeeking && System.currentTimeMillis() - lastSeekTime > 1000) {
                currentPosition = exoPlayer.currentPosition
            }
            delay(500)
        }
    }

    // Auto-hide controls
    LaunchedEffect(isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying) {
            delay(3000)
            isControlsVisible = false
        }
    }

    // Hide feedbacks
    LaunchedEffect(volumeLevel, brightnessLevel, seekText) {
        if (volumeLevel != null || brightnessLevel != null || seekText != null) {
            delay(1000)
            volumeLevel = null
            brightnessLevel = null
            seekText = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!isExiting) {
            // 1. Video Surface
            AndroidView(
                factory = {
                    PlayerView(context).apply {
                        player = exoPlayer
                        useController = false // Hide native controller
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Gesture Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isScreenLocked) {
                    detectTapGestures(
                        onTap = { isControlsVisible = !isControlsVisible },
                        onDoubleTap = {
                            if (isScreenLocked) return@detectTapGestures
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        }
                    )
                }
                .pointerInput(isScreenLocked) {
                    if (isScreenLocked) return@pointerInput
                    var startX = 0f
                    var startY = 0f
                    var startVolume = 0
                    var startBrightness = 0f
                    var isLeftEdge = false
                    var dragDirection = 0 // 0: none, 1: horizontal, 2: vertical
                    var startPosition = 0L

                    detectDragGestures(
                        onDragStart = { offset ->
                            startX = offset.x
                            startY = offset.y
                            isLeftEdge = offset.x < size.width / 2
                            dragDirection = 0
                            startPosition = exoPlayer.currentPosition
                            
                            if (isLeftEdge) {
                                startBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
                                if (startBrightness < 0) startBrightness = 0.5f // Fallback
                            } else {
                                startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            }
                        },
                        onDragEnd = {
                            if (dragDirection == 1) {
                                exoPlayer.seekTo(currentPosition)
                                lastSeekTime = System.currentTimeMillis()
                                isSeeking = false
                            }
                            dragDirection = 0
                        },
                        onDragCancel = {
                            if (dragDirection == 1) {
                                exoPlayer.seekTo(currentPosition)
                                lastSeekTime = System.currentTimeMillis()
                                isSeeking = false
                            }
                            dragDirection = 0
                        },
                        onDrag = { change, _ ->
                            if (dragDirection == 0) {
                                val dx = Math.abs(change.position.x - startX)
                                val dy = Math.abs(change.position.y - startY)
                                if (dx > 50f || dy > 50f) {
                                    dragDirection = if (dx > dy) 1 else 2
                                }
                            }

                            if (dragDirection == 1) {
                                // Horizontal Seek
                                isSeeking = true
                                val deltaX = change.position.x - startX
                                // Swipe full screen width = 3 minutes (180,000 ms)
                                val seekMs = (deltaX / size.width) * 180000L
                                val targetPos = (startPosition + seekMs).toLong().coerceIn(0L, duration)
                                currentPosition = targetPos
                                
                                val diff = targetPos - startPosition
                                val sign = if (diff >= 0) "+" else "-"
                                seekText = "$sign${formatMinSec(Math.abs(diff))}\n${formatMinSec(targetPos)} / ${formatMinSec(duration)}"
                            } else if (dragDirection == 2) {
                                // Vertical Volume/Brightness
                                val deltaY = startY - change.position.y
                                val deltaRatio = deltaY / size.height.toFloat()

                                if (isLeftEdge) {
                                    // Brightness adjustment
                                    val newBrightness = (startBrightness + deltaRatio * 2).coerceIn(0f, 1f)
                                    activity?.window?.let { win ->
                                        val lp = win.attributes
                                        lp.screenBrightness = newBrightness
                                        win.attributes = lp
                                    }
                                    brightnessLevel = newBrightness
                                } else {
                                    // Volume adjustment
                                    val newVolume = (startVolume + deltaRatio * maxVolume * 1.5f).toInt().coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                    volumeLevel = newVolume.toFloat() / maxVolume
                                }
                            }
                        }
                    )
                }
        )

        // 3. Custom UI Controls
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                if (!isScreenLocked) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 14.dp)
                        ) {
                            PlayerIconButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                onClick = { performExit() }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentVideoTitle,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        PlayerIconButton(
                            icon = if (isRotationLocked) Icons.Default.ScreenLockRotation else Icons.Default.ScreenRotation,
                            contentDescription = "锁定旋转",
                            selected = isRotationLocked,
                            onClick = {
                                isRotationLocked = !isRotationLocked
                                if (isRotationLocked) {
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                                } else {
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                }
                            }
                        )
                    }

                    // Bottom Bar Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 18.dp, vertical = 18.dp)
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        // Time & Progress
                        Text(
                            text = "${formatMinSec(currentPosition)}/${formatMinSec(duration)}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { isSeeking = true },
                                        onDragEnd = {
                                            exoPlayer.seekTo(currentPosition)
                                            lastSeekTime = System.currentTimeMillis()
                                            isSeeking = false
                                        },
                                        onDragCancel = {
                                            exoPlayer.seekTo(currentPosition)
                                            lastSeekTime = System.currentTimeMillis()
                                            isSeeking = false
                                        }
                                    ) { change, dragAmount ->
                                        if (duration > 0) {
                                            val deltaMs = (dragAmount / size.width) * duration
                                            currentPosition = (currentPosition + deltaMs).toLong().coerceIn(0L, duration)
                                        }
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = { offset ->
                                            if (duration > 0) {
                                                isSeeking = true
                                                currentPosition = ((offset.x / size.width) * duration).toLong().coerceIn(0L, duration)
                                                val press = tryAwaitRelease()
                                                if (press) {
                                                    exoPlayer.seekTo(currentPosition)
                                                    lastSeekTime = System.currentTimeMillis()
                                                }
                                                isSeeking = false
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
                            // Inactive Track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(2.dp))
                            )
                            // Active Track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progress)
                                    .height(4.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                            )
                            // Thumb
                            Box(
                                modifier = Modifier
                                    .offset(x = (maxWidth - 12.dp) * progress)
                                    .size(12.dp)
                                    .background(Color.White, CircleShape)
                            )
                        }

                        // Bottom Controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(22.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerIconButton(
                                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "播放或暂停",
                                    prominent = true,
                                    onClick = { if (isPlaying) exoPlayer.pause() else exoPlayer.play() }
                                )

                                PlayerIconButton(
                                    icon = Icons.Default.SkipNext,
                                    contentDescription = "下一个视频",
                                    onClick = {
                                        val currentIndex = playlist.indexOfFirst { it.uri == currentVideoUri }
                                        if (currentIndex in 0 until playlist.size - 1) {
                                            val nextVideo = playlist[currentIndex + 1]
                                            currentVideoUri = nextVideo.uri
                                            currentVideoTitle = nextVideo.title

                                            if (!isRotationLocked && nextVideo.width > 0 && nextVideo.height > 0) {
                                                if (nextVideo.width > nextVideo.height) {
                                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                                } else {
                                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                                }
                                            }
                                        }
                                    }
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(26.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerIconButton(
                                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                                    contentDescription = "播放列表",
                                    selected = showPlaylist,
                                    onClick = { showPlaylist = true }
                                )

                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .bounceClick {
                                            playbackSpeed = when (playbackSpeed) {
                                                1f -> 1.25f
                                                1.25f -> 1.5f
                                                1.5f -> 2f
                                                else -> 1f
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${playbackSpeed}x",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Middle Right Lock Button
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .size(42.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.Black.copy(alpha = 0.48f))
                        .bounceClick { isScreenLocked = !isScreenLocked },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isScreenLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock Screen",
                        tint = Color.White
                    )
                }
            }
        }

        // 4. Feedback Overlays
        seekText?.let {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = it,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                        .background(Color.Black.copy(0.64f), shape = MaterialTheme.shapes.large)
                        .padding(24.dp)
                )
            }
        }

        if (volumeLevel != null || brightnessLevel != null) {
            val text = if (volumeLevel != null) {
                "音量 ${(volumeLevel!! * 100).toInt()}%"
            } else {
                "亮度 ${(brightnessLevel!! * 100).toInt()}%"
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .background(Color.Black.copy(0.64f), shape = MaterialTheme.shapes.medium)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Playlist Drawer
        if (showPlaylist) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { showPlaylist = false })
                    }
            )
        }

        AnimatedVisibility(
            visible = showPlaylist,
            enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }),
            exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .background(Color(0xEE101820))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("播放列表", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { showPlaylist = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close", tint = Color.White)
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = playlist,
                            key = { it.id },
                            contentType = { "playlist_video" }
                        ) { video ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(if (video.uri == currentVideoUri) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f) else Color.Transparent)
                                    .clickable {
                                        currentVideoUri = video.uri
                                        currentVideoTitle = video.title
                                        showPlaylist = false
                                        
                                        // Update orientation immediately for the new video
                                        if (!isRotationLocked && video.width > 0 && video.height > 0) {
                                            if (video.width > video.height) {
                                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                            } else {
                                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                                            }
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                        .data(video.uri)
                                        .crossfade(true)
                                        .size(256) // Small size for playlist thumbnail
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(80.dp, 45.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.DarkGray)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = video.title,
                                        color = if (video.uri == currentVideoUri) MaterialTheme.colorScheme.primary else Color.White,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2
                                    )
                                    Text(
                                        text = formatMinSec(video.duration),
                                        color = Color.White.copy(alpha = 0.62f),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selected: Boolean = false,
    prominent: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(if (prominent) 44.dp else 40.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                when {
                    prominent -> MaterialTheme.colorScheme.primary
                    selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    else -> Color.White.copy(alpha = 0.10f)
                }
            )
            .bounceClick(scaleDown = 0.94f) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(if (prominent) 26.dp else 22.dp)
        )
    }
}

private fun formatMinSec(timeMs: Long): String {
    if (timeMs < 0) return "00:00"
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(timeMs)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
