package com.muswall.lite.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatusActionCard(
    hasUnsavedChanges: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasUnsavedChanges) Color(0xFF1B2230) else MaterialTheme.colorScheme.surface
        ),
        border = if (hasUnsavedChanges) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasUnsavedChanges) "Unsaved Modifications" else "Configuration Status",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (hasUnsavedChanges) MaterialTheme.colorScheme.primary else Color.White
                )
                if (hasUnsavedChanges) {
                    Text(
                        text = "● Pending Apply",
                        color = Color(0xFFFFB74D),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "✓ Synced",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSave,
                    enabled = hasUnsavedChanges,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = Color(0xFF282836)
                    )
                ) {
                    Text(
                        text = if (hasUnsavedChanges) "Save & Apply Changes" else "All Changes Saved",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasUnsavedChanges) Color.Black else Color.Gray
                    )
                }

                if (hasUnsavedChanges) {
                    OutlinedButton(
                        onClick = onDiscard,
                        modifier = Modifier.height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Discard", fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
