package com.ultraviolette.navigator.ui.components

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import com.ultraviolette.navigator.model.ClusterPlaceholders
import com.ultraviolette.navigator.ui.theme.clusterColors
import kotlin.math.max

/** Minimum black clearance so UI clears phone rounded corners on short edges. */
val ClusterMinEdgeBezel: Dp = 28.dp

private val TopBarMotionMs = 280

/**
 * Shared landscape shell: short-edge bezels + top status bar + content below.
 * Speedometer and Control Centre both sit inside this frame.
 *
 * [showTopBar] slides the status strip up and frees the content band
 * (used by Settings → Themes for a fuller brand grid).
 */
@Composable
fun ClusterFrame(
    data: ClusterPlaceholders,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = clusterColors()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val view = LocalView.current
    val cutout = WindowInsets.displayCutout
    val cutoutLeft = with(density) { cutout.getLeft(this, layoutDirection).toDp() }
    val cutoutRight = with(density) { cutout.getRight(this, layoutDirection).toDp() }

    val rounded = remember(view, view.width, view.height) {
        edgeRoundedCornerPx(view)
    }
    val leftRound = with(density) { rounded.left.toDp() }
    val rightRound = with(density) { rounded.right.toDp() }

    val leftBezel = maxOf(ClusterMinEdgeBezel, cutoutLeft, leftRound)
    val rightBezel = maxOf(ClusterMinEdgeBezel, cutoutRight, rightRound)
    val barFade = tween<Float>(TopBarMotionMs, easing = FastOutSlowInEasing)
    val barSlide = tween<IntOffset>(TopBarMotionMs, easing = FastOutSlowInEasing)
    val barSize = tween<IntSize>(TopBarMotionMs, easing = FastOutSlowInEasing)

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bezel)
    ) {
        EdgeBezel(leftBezel)

        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(colors.background)
        ) {
            AnimatedVisibility(
                visible = showTopBar,
                enter = fadeIn(barFade) +
                    slideInVertically(animationSpec = barSlide) { -it } +
                    expandVertically(animationSpec = barSize, expandFrom = Alignment.Top),
                exit = fadeOut(barFade) +
                    slideOutVertically(animationSpec = barSlide) { -it } +
                    shrinkVertically(animationSpec = barSize, shrinkTowards = Alignment.Top)
            ) {
                TopStatusBar(data = data)
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                content = content
            )
        }

        EdgeBezel(rightBezel)
    }
}

@Composable
private fun EdgeBezel(width: Dp) {
    val colors = clusterColors()
    Spacer(
        Modifier
            .width(width)
            .fillMaxHeight()
            .background(colors.bezel)
    )
}

private data class EdgeRounds(val left: Int, val right: Int)

private fun edgeRoundedCornerPx(view: android.view.View): EdgeRounds {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return EdgeRounds(0, 0)
    }
    val insets = ViewCompat.getRootWindowInsets(view)?.toWindowInsets() ?: return EdgeRounds(0, 0)
    val left = max(
        insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0,
        insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0
    )
    val right = max(
        insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)?.radius ?: 0,
        insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0
    )
    return EdgeRounds(left, right)
}
