package com.example.isplayer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

sealed class BottomNavItem(val route: String, val icon: ImageVector, val selectedIcon: ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Outlined.Home, Icons.Filled.Home, "HOME")
    object Library : BottomNavItem("library", Icons.Outlined.VideoLibrary, Icons.Filled.VideoLibrary, "LIBRARY")
    object Settings : BottomNavItem("settings", Icons.Outlined.Settings, Icons.Filled.Settings, "SETTINGS")
}

@Composable
fun CinematicBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(BottomNavItem.Home, BottomNavItem.Library, BottomNavItem.Settings)

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(32.dp))
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) // glow effect
                )
            )
        }
    }
}
