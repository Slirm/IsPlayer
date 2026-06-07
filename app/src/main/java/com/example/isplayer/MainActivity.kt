package com.example.isplayer

import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.isplayer.domain.model.LocalVideo
import com.example.isplayer.presentation.home.HomeScreen
import com.example.isplayer.presentation.player.PlayerScreen
import com.example.isplayer.ui.theme.IsPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IsPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onVideoClick = { video ->
                                    val encodedUri = java.net.URLEncoder.encode(java.net.URLEncoder.encode(video.uri, "UTF-8"), "UTF-8")
                                    val encodedTitle = java.net.URLEncoder.encode(java.net.URLEncoder.encode(video.title, "UTF-8"), "UTF-8")
                                    navController.navigate("player/$encodedUri/$encodedTitle/${video.folderId}?width=${video.width}&height=${video.height}")
                                }
                            )
                        }
                        
                        composable(
                            route = "player/{videoUri}/{videoTitle}/{folderId}?width={width}&height={height}",
                            arguments = listOf(
                                androidx.navigation.navArgument("videoUri") { type = androidx.navigation.NavType.StringType },
                                androidx.navigation.navArgument("videoTitle") { type = androidx.navigation.NavType.StringType },
                                androidx.navigation.navArgument("folderId") { type = androidx.navigation.NavType.StringType },
                                androidx.navigation.navArgument("width") { 
                                    type = androidx.navigation.NavType.IntType 
                                    defaultValue = 0
                                },
                                androidx.navigation.navArgument("height") { 
                                    type = androidx.navigation.NavType.IntType 
                                    defaultValue = 0
                                }
                            )
                        ) { backStackEntry ->
                            val encodedUri = backStackEntry.arguments?.getString("videoUri") ?: ""
                            val videoUri = if (encodedUri.isNotEmpty()) java.net.URLDecoder.decode(encodedUri, "UTF-8") else ""
                            
                            val encodedTitle = backStackEntry.arguments?.getString("videoTitle") ?: ""
                            val videoTitle = if (encodedTitle.isNotEmpty()) java.net.URLDecoder.decode(encodedTitle, "UTF-8") else "Unknown"
                            
                            val folderId = backStackEntry.arguments?.getString("folderId")?.toLongOrNull() ?: 1L
                            val width = backStackEntry.arguments?.getInt("width") ?: 0
                            val height = backStackEntry.arguments?.getInt("height") ?: 0
                            
                            PlayerScreen(
                                videoUri = videoUri,
                                videoTitle = videoTitle,
                                folderId = folderId,
                                initialWidth = width,
                                initialHeight = height,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
