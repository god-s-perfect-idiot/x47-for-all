package com.ultraviolette.navigator.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Slide distance for each tile entering from the right. */
val ClusterTileEnterSlide = 56.dp
const val ClusterTileEnterDurationMs = 320
/** Stagger between consecutive tiles (L→R). */
const val ClusterTileEnterStaggerMs = 55
/** Extra pause before the next visual row starts. */
const val ClusterTileEnterRowGapMs = 70

/**
 * Reading-order enter delay for a fixed-column grid:
 * row-major L→R, top→bottom, with a gap between rows.
 */
fun clusterTileEnterDelayMs(order: Int, colsPerRow: Int): Int {
    val row = order / colsPerRow
    val col = order % colsPerRow
    return row * (colsPerRow * ClusterTileEnterStaggerMs + ClusterTileEnterRowGapMs) +
        col * ClusterTileEnterStaggerMs
}

/** Linear stagger — good for header rows and short tile lists. */
fun clusterTileEnterLinearDelayMs(order: Int): Int = order * ClusterTileEnterStaggerMs

/** Slide in from the right + fade, delayed by [delayMs]. */
@Composable
fun Modifier.clusterTileEnter(delayMs: Int): Modifier {
    val density = LocalDensity.current
    val slidePx = with(density) { ClusterTileEnterSlide.toPx() }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(delayMs) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = ClusterTileEnterDurationMs,
                delayMillis = delayMs,
                easing = FastOutSlowInEasing
            )
        )
    }
    return this.graphicsLayer {
        translationX = (1f - progress.value) * slidePx
        alpha = progress.value
    }
}
