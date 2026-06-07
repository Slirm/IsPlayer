package com.example.isplayer.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.isplayer.ui.theme.PrimaryBlue
import com.example.isplayer.ui.theme.TagOrange
import com.example.isplayer.utils.bounceClick

@Composable
fun VideoItemCard(
    title: String,
    duration: String,
    size: String,
    date: String,
    thumbnailUri: String?, // Or use File path
    is4K: Boolean = false,
    progress: Float = 0.3f, // Fake progress for now
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .bounceClick(scaleDown = 0.95f, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnailUri)
                        .crossfade(true)
                        .size(512) // Optimize for thumbnail
                        .build(),
                    contentDescription = "Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tag
                    Box(
                        modifier = Modifier
                            .background(TagOrange, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (is4K) "4K QHD" else "1080P FHD",
                            color = Color(0xFF333333),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = duration,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(16.dp),
                        color = PrimaryBlue,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}
