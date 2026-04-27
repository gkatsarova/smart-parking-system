package com.planzy.smartparkingsystem.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.planzy.smartparkingsystem.data.ParkingState
import com.planzy.smartparkingsystem.ui.theme.*
import com.planzy.smartparkingsystem.viewmodel.CaptureStatus

@Composable
fun SpotsRow(parking: ParkingState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SpotCard(
            modifier = Modifier.weight(1f),
            count = parking.free,
            label = "Free",
            bg = Green50,
            fg = Green700,
        )
        SpotCard(
            modifier = Modifier.weight(1f),
            count = parking.occupied,
            label = "Occupied",
            bg = Red50,
            fg = Red700,
        )
    }
}

@Composable
private fun SpotCard(
    modifier: Modifier,
    count: Int,
    label: String,
    bg: Color,
    fg: Color,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = count.toString(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = fg.copy(alpha = 0.75f),
                letterSpacing = 0.8.sp,
            )
        }
    }
}

@Composable
fun ParkingStatusRow(parking: ParkingState) {
    val (dotColor, statusText) = when {
        parking.free > 5 -> Green700 to "Lot of free spots"
        parking.free > 0 -> Amber600 to "Little bit busy"
        else -> Red700 to "Parking is full"
    }

    val animatedColor by animateColorAsState(
        targetValue = dotColor,
        animationSpec = tween(600),
        label = "statusColor",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(animatedColor),
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (parking.confidence > 0) {
            Spacer(Modifier.weight(1f))
            Text(
                text = "AI ${parking.confidence}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            )
        }
    }
}

@Composable
fun LastUpdateText(timestamp: Long) {
    val text = if (timestamp == 0L) {
        "Loading"
    } else {
        val secondsAgo = (System.currentTimeMillis() - timestamp) / 1000
        "Refreshed " + when {
            secondsAgo < 60 -> "${secondsAgo}s go"
            secondsAgo < 3600 -> "${secondsAgo / 60}mins ago"
            else -> "${secondsAgo / 3600}h ago"
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
    )
}

@Composable
fun CaptureButton(
    status: CaptureStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (containerColor, contentColor, text, icon, enabled) = when (status) {
        CaptureStatus.Idle -> CaptureButtonStyle(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
            "Capturing the parking",
            { Icon(Icons.Default.CameraAlt, null) },
            true,
        )
        CaptureStatus.Capturing -> CaptureButtonStyle(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurface.copy(0.38f),
            "Taking a photo",
            { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) },
            false,
        )
        CaptureStatus.Sent      -> CaptureButtonStyle(
            Green700,
            Color.White,
            "Sent",
            { Icon(Icons.Default.CheckCircle, null) },
            false,
        )
        is CaptureStatus.Failed -> CaptureButtonStyle(
            Red700,
            Color.White,
            "Error - try again",
            { Icon(Icons.Default.Error, null) },
            true,
        )
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor,
        ),
    ) {
        icon()
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Medium)
    }
}

private data class CaptureButtonStyle(
    val containerColor: Color,
    val contentColor: Color,
    val text: String,
    val icon: @Composable () -> Unit,
    val enabled: Boolean,
)

@Composable
fun DistanceChip(distanceMeters: Float, inRadius: Boolean) {
    val label = when {
        distanceMeters == Float.MAX_VALUE -> "GPS: localizing..."
        inRadius -> "In radius"
        distanceMeters < 1000 -> "%.0fm to the parking".format(distanceMeters)
        else -> "%.1fkm to the parking".format(distanceMeters / 1000)
    }
    val color = if (inRadius) Green700 else MaterialTheme.colorScheme.onSurface.copy(0.6f)
    val bg = if (inRadius) Green50  else MaterialTheme.colorScheme.surfaceVariant

    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}