package com.example.isplayer.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.isplayer.R
import com.example.isplayer.domain.model.Folder
import com.example.isplayer.utils.bounceClick

@Composable
fun DrawerContent(
    folders: List<Folder>,
    currentFolderId: Long,
    currentFolderVideoCount: Int,
    onFolderClick: (Long) -> Unit,
    onImportVideoClick: () -> Unit,
    onImportFromGalleryClick: () -> Unit,
    onImportFromDownloadClick: () -> Unit,
    onAddFolderClick: () -> Unit
) {
    var isImportMenuExpanded by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 32.dp)
        ) {
            // User Header (Pinned to top)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp, start = 8.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF07111D),
                                    Color(0xFF123B64),
                                    Color(0xFF1B5DAB)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "IsPlayer",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "本地视频库",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Scrollable Middle Content (Import/Add + Folders)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Action Cards 1: Import & Add Folder
                Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Box {
                        DrawerActionItem(
                            icon = Icons.Outlined.AddBox, 
                            text = "导入视频", 
                            onClick = { isImportMenuExpanded = true }
                        )
                        
                        DropdownMenu(
                            expanded = isImportMenuExpanded,
                            onDismissRequest = { isImportMenuExpanded = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .width(240.dp),
                            offset = androidx.compose.ui.unit.DpOffset(16.dp, 0.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("从文件管理器导入", fontSize = 14.sp) },
                                onClick = { 
                                    isImportMenuExpanded = false
                                    onImportVideoClick() 
                                },
                                leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                            DropdownMenuItem(
                                text = { Text("从相册导入", fontSize = 14.sp) },
                                onClick = { 
                                    isImportMenuExpanded = false
                                    onImportFromGalleryClick() 
                                },
                                leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                            DropdownMenuItem(
                                text = { Text("从下载文件夹导入", fontSize = 14.sp) },
                                onClick = { 
                                    isImportMenuExpanded = false
                                    onImportFromDownloadClick() 
                                },
                                leadingIcon = { Icon(Icons.Outlined.SnippetFolder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                        }
                    }

                    DrawerActionItem(icon = Icons.Outlined.CreateNewFolder, text = "添加文件夹", onClick = onAddFolderClick)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Folders List (Height scales with content because there's no weight)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    folders.forEach { folder ->
                        val isSelected = folder.id == currentFolderId
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .bounceClick { onFolderClick(folder.id) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Outlined.SnippetFolder else Icons.Outlined.Folder,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = folder.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Text(
                                            "共 $currentFolderVideoCount 部视频",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            } // End of Scrollable Middle Content

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Actions: Recent Play & Settings (Pinned to bottom)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    DrawerActionItem(icon = Icons.Outlined.Schedule, text = "最近播放", onClick = { })
                    DrawerActionItem(icon = Icons.Outlined.Settings, text = "设置", onClick = { })
                }
            }
        }
    }
}

@Composable
private fun DrawerActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
