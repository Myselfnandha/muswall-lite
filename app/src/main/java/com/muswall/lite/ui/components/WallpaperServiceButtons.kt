package com.muswall.lite.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WallpaperServiceButtons(
    isWallpaperActive: Boolean,
    isNotificationGranted: Boolean,
    onSetWallpaperClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onAppInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onSetWallpaperClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isWallpaperActive) Color(0xFF1E2838) else MaterialTheme.colorScheme.primary
            ),
            border = if (isWallpaperActive) BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary) else null
        ) {
            Text(
                text = if (isWallpaperActive) "✓ Live Wallpaper Active (Re-apply)" else "Set as Live Wallpaper",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isWallpaperActive) MaterialTheme.colorScheme.secondary else Color.Black
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onNotificationSettingsClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (isNotificationGranted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else Color(0xFFFFB74D)
                )
            ) {
                Text(
                    text = if (isNotificationGranted) "✓ Notification Access" else "⚠️ Notification Access",
                    fontSize = 11.sp,
                    color = if (isNotificationGranted) MaterialTheme.colorScheme.secondary else Color(0xFFFFB74D),
                    textAlign = TextAlign.Center
                )
            }

            OutlinedButton(
                onClick = onAppInfoClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("App Info (Restricted / Battery)", fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
