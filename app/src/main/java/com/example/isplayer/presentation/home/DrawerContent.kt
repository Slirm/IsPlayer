package com.example.isplayer.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.isplayer.domain.model.Folder
import com.example.isplayer.ui.theme.PrimaryBlue
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
        drawerContainerColor = Color(0xFFF2F4F6), // Light gray background like the screenshot
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
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("流心晚上好", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            // Scrollable Middle Content (Import/Add + Folders)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Action Cards 1: Import & Add Folder
                Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
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
                                .background(Color.White)
                                .width(240.dp),
                            offset = androidx.compose.ui.unit.DpOffset(16.dp, 0.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("从文件管理器导入", fontSize = 14.sp) },
                                onClick = { 
                                    isImportMenuExpanded = false
                                    onImportVideoClick() 
                                },
                                leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null, tint = Color.DarkGray) }
                            )
                            DropdownMenuItem(
                                text = { Text("从相册导入", fontSize = 14.sp) },
                                onClick = { 
                                    isImportMenuExpanded = false
                                    onImportFromGalleryClick() 
                                },
                                leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null, tint = Color.DarkGray) }
                            )
                            DropdownMenuItem(
                                text = { Text("从下载文件夹导入", fontSize = 14.sp) },
                                onClick = { 
                                    isImportMenuExpanded = false
                                    onImportFromDownloadClick() 
                                },
                                leadingIcon = { Icon(Icons.Outlined.SnippetFolder, contentDescription = null, tint = Color.DarkGray) }
                            )
                        }
                    }

                    DrawerActionItem(icon = Icons.Outlined.CreateNewFolder, text = "添加文件夹", onClick = onAddFolderClick)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Folders List (Height scales with content because there's no weight)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    folders.forEach { folder ->
                        val isSelected = folder.id == currentFolderId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick { onFolderClick(folder.id) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Outlined.SnippetFolder else Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = if (isSelected) PrimaryBlue else Color.DarkGray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = folder.name,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PrimaryBlue else Color.Black
                                )
                                if (isSelected) {
                                    Text("共 $currentFolderVideoCount 部视频", fontSize = 12.sp, color = PrimaryBlue)
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
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
        Icon(imageVector = icon, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, fontSize = 16.sp, color = Color.Black, modifier = Modifier.weight(1f))
        if (trailingIcon != null) {
            Icon(imageVector = trailingIcon, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(24.dp))
        }
    }
}
