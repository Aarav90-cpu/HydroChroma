package com.arkstudios.hydrochroma

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp

/**
 * 💥 Shockwave Controller
 * Use this to trigger the shockwave effect from anywhere.
 */
@Stable
class ShockwaveState {
    internal val progress = Animatable(0f)
    internal var center by mutableStateOf(Offset.Zero)
    internal var isActive by mutableStateOf(false)

    /**
     * Trigger a chromatic shockwave.
     * @param origin The screen coordinates where the shockwave starts.
     * If null, it starts from the bottom-center.
     */
    suspend fun trigger(origin: Offset? = null) {
        // Set origin (or default to a flag that we resolve later)
        if (origin != null) {
            center = origin
        }

        isActive = true
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(800)
        )
        // Reset after animation
        isActive = false
        progress.snapTo(0f)
    }
}

@Composable
fun rememberShockwaveState(): ShockwaveState {
    return remember { ShockwaveState() }
}

/**
 * 🌊 ShockwaveContainer
 * Wraps your entire screen content and applies a chromatic shockwave effect
 * when triggered via [ShockwaveState].
 *
 * Usage:
 * ```
 * val shockwaveState = rememberShockwaveState()
 * * ShockwaveContainer(state = shockwaveState) {
 * Button(onClick = {
 * coroutineScope.launch { shockwaveState.trigger() }
 * }) { Text("BOOM") }
 * }
 * ```
 */
@Composable
fun ShockwaveContainer(
    state: ShockwaveState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var screenSize by remember { mutableStateOf(Offset.Zero) }

    // Resolve default center if not set
    val activeCenter = if (state.center == Offset.Zero) {
        Offset(screenSize.x / 2f, screenSize.y * 0.9f) // Bottom Center default
    } else {
        state.center
    }

    val shockwaveModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Modifier.hydroChroma(
            chromaticConfig = if (state.isActive) chromaticAberration(
                listOf(Color.Cyan, Color.Magenta, Color.Yellow)
            ) else noChromaticConfig(), // Only apply color when active

            manualRippleProgress = state.progress.value,
            manualTouchPosition = activeCenter,

            animations = animations(onClick = HydroAction.Expand),

            effects = effects(
                chromaticDensity = 2.5f, // Strong glitch
                fluidity = 0.8f, // Fast shockwave
                maxTilt = 0f, // No tilt needed
                blur = 0.dp
            ),
            isBackgroundLiquidGlass = false // Distort everything!
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                screenSize = Offset(it.width.toFloat(), it.height.toFloat())
            }
            .then(shockwaveModifier),
        content = content
    )
}