package com.ultraviolette.navigator.ui.ktm

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

/** KTM menu rows enter from the left — opposite of UV tile stagger. */
val KtmMenuEnterSlide = 48.dp
const val KtmMenuEnterDurationMs = 280
const val KtmMenuEnterStaggerMs = 48
const val KtmTachSweepMs = 420
const val KtmPaneMotionMs = 300

fun ktmMenuEnterDelayMs(order: Int): Int = order * KtmMenuEnterStaggerMs

@Composable
fun Modifier.ktmMenuEnter(delayMs: Int): Modifier {
    val density = LocalDensity.current
    val slidePx = with(density) { KtmMenuEnterSlide.toPx() }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(delayMs) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = KtmMenuEnterDurationMs,
                delayMillis = delayMs,
                easing = FastOutSlowInEasing
            )
        )
    }
    return this.graphicsLayer {
        translationX = (1f - progress.value) * -slidePx
        alpha = progress.value
    }
}

@Composable
fun Modifier.ktmPreviewEnter(delayMs: Int = 120): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(delayMs) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = KtmMenuEnterDurationMs + 40,
                delayMillis = delayMs,
                easing = FastOutSlowInEasing
            )
        )
    }
    return this.graphicsLayer {
        translationX = (1f - progress.value) * 36f
        alpha = progress.value
        scaleX = 0.96f + 0.04f * progress.value
        scaleY = 0.96f + 0.04f * progress.value
    }
}
