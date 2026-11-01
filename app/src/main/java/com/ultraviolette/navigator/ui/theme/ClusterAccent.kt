package com.ultraviolette.navigator.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.ultraviolette.navigator.model.ClusterPlaceholders

data class ClusterAccent(
    val primary: Color,
    val label: Color
)

/**
 * Ride-mode accent: GLIDE green, COMBAT blue, BALLISTIC red.
 * Drives side labels, control centre, top chevron, and the moving mode bar.
 */
@Composable
fun rememberClusterAccent(data: ClusterPlaceholders): ClusterAccent {
    return remember(data.rideMode) {
        val color = when {
            data.isGlide -> ClusterGlide
            data.isBallistic -> ClusterBallistic
            else -> ClusterBlue
        }
        ClusterAccent(primary = color, label = color.copy(alpha = 0.85f))
    }
}
