package com.ultraviolette.navigator.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.ultraviolette.navigator.model.ClusterPlaceholders

data class ClusterAccent(
    val primary: Color,
    val label: Color
)

@Composable
fun rememberClusterAccent(data: ClusterPlaceholders): ClusterAccent {
    return remember(data.rideMode) {
        if (data.isBallistic) {
            ClusterAccent(primary = ClusterRed, label = ClusterRed)
        } else {
            ClusterAccent(primary = ClusterBlue, label = ClusterBlue.copy(alpha = 0.85f))
        }
    }
}
