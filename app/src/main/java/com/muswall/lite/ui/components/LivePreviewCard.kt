package com.muswall.lite.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LivePreviewCard(
    trackTitle: String?,
    previewShowingRestore: Boolean,
    homePreviewBitmap: Bitmap,
    lockPreviewBitmap: Bitmap,
    activeTab: Int,
    onTabSelect: (Int) -> Unit,
    onTogglePreviewRestore: (Boolean) -> Unit,
    onCycleDemoArt: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Live Screen Preview",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (previewShowingRestore) "Restored Default Wallpaper (Home & Lock)" else (trackTitle ?: "Music Album Artwork"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onCycleDemoArt != null && !previewShowingRestore) {
                        AssistChip(
                            onClick = onCycleDemoArt,
                            label = { Text("🔀 Cover", fontSize = 11.sp) }
                        )
                    }
                    FilterChip(
                        selected = !previewShowingRestore,
                        onClick = { onTogglePreviewRestore(false) },
                        label = { Text("🎵 Music", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = previewShowingRestore,
                        onClick = { onTogglePreviewRestore(true) },
                        label = { Text("🖼️ Recover", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Home Screen Mini Phone Mockup
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onTabSelect(0) }
                ) {
                    Box(
                        modifier = Modifier
                            .width(105.dp)
                            .height(225.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(
                                width = if (activeTab == 0) 2.5.dp else 1.dp,
                                color = if (activeTab == 0) MaterialTheme.colorScheme.primary else Color(0xFF333345),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = homePreviewBitmap.asImageBitmap(),
                            contentDescription = "Home Screen Live Preview",
                            modifier = Modifier.fillMaxSize()
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 10.dp)
                                    .width(55.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x35FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("12:45", fontSize = 8.sp, color = Color(0x99FFFFFF))
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0x30FFFFFF))
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    repeat(4) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x77FFFFFF))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🏠 Home Screen",
                        fontSize = 11.sp,
                        fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (activeTab == 0) MaterialTheme.colorScheme.primary else Color.LightGray
                    )
                }

                // Lock Screen Mini Phone Mockup
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onTabSelect(1) }
                ) {
                    Box(
                        modifier = Modifier
                            .width(105.dp)
                            .height(225.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(
                                width = if (activeTab == 1) 2.5.dp else 1.dp,
                                color = if (activeTab == 1) MaterialTheme.colorScheme.primary else Color(0xFF333345),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = lockPreviewBitmap.asImageBitmap(),
                            contentDescription = "Lock Screen Live Preview",
                            modifier = Modifier.fillMaxSize()
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "12:45",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tuesday, Nov 10",
                                color = Color(0xCCFFFFFF),
                                fontSize = 9.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🔒 Lock Screen",
                        fontSize = 11.sp,
                        fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (activeTab == 1) MaterialTheme.colorScheme.primary else Color.LightGray
                    )
                }
            }
        }
    }
}
