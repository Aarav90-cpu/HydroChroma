package com.arkstudios.hydrochroma.components

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.arkstudios.hydrochroma.HydroAction
import com.arkstudios.hydrochroma.HydroAnimations
import com.arkstudios.hydrochroma.ShockwaveState
import com.arkstudios.hydrochroma.animations
import com.arkstudios.hydrochroma.chromaticAberration
import com.arkstudios.hydrochroma.effects
import com.arkstudios.hydrochroma.hydroChroma
import com.arkstudios.hydrochroma.noLiquidGlass
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.launch

/**
 * 🌊 HydroBottomNavigation
 *
 * A static liquid glass bottom bar with a sliding liquid pill.
 * Integrates with [ShockwaveState] to trigger screen-wide effects on tap.
 *
 * @param shockwaveState The controller to trigger screen shockwaves.
 */
@Composable
fun HydroBottomNavigation(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabsCount: Int,
    shockwaveState: ShockwaveState? = null, // 👈 New Parameter
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()
    val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f) else Color(0xFF121212).copy(0.4f)
    val scope = rememberCoroutineScope()
    val backdrop = rememberLayerBackdrop()

    Box(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(80.dp)
    ) {
        // 1. The Bar Container
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.Center)
                    .hydroChroma(
                        liquidGlass = noLiquidGlass(),
                        chromaticConfig = null,
                        backdrop = backdrop,
                        animations = HydroAnimations(
                            animateAction = HydroAction.Fluid
                        )
                    )
            )
        }

        // 2. The Active Pill
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.Center)
        ) {
            val tabWidth = maxWidth / tabsCount

            // Slide Animation
            val indicatorOffset by animateFloatAsState(
                targetValue = selectedTabIndex.toFloat(),
                animationSpec = tween(500, easing = FastOutSlowInEasing),
                label = "PillSlide"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Morphing Shape Animation
                val infiniteTransition = rememberInfiniteTransition(label = "ShapeMorph")
                val morphProgress by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "Morph"
                )

                // Define varying shapes based on time
                // This is a simple approximation. For true shape morphing, we'd use a custom Path.
                // Here we oscillate the corner radius to simulate Circle -> Rect
                val cornerRadius by infiniteTransition.animateFloat(
                    initialValue = 100f, // Circle (approx)
                    targetValue = 10f,  // Rect-ish
                    animationSpec = infiniteRepeatable(
                        animation = tween(100, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "CornerMorph"
                )

                Box(
                    Modifier
                        .width(tabWidth)
                        .fillMaxHeight()
                        .padding(6.dp)
                        .offset { IntOffset((indicatorOffset * tabWidth.toPx()).toInt(), 0) }
                        // 🌊 Hydro Effect on the Pill itself
                        .hydroChroma(
                            animations = animations(
                                onAnimate = HydroAction.Morph // Constant glitchy breathing
                            ),
                            effects = effects(
                                fluidity = 1.5f,
                                chromaticDensity = 1.2f, // Visible glitch on the pill
                                morphStrength = 0.3f // Noticeable morph
                            ),
                            chromaticConfig = chromaticAberration(listOf(Color.Cyan, Color.Magenta)),
                            isBackgroundLiquidGlass = true,
                            shape = ContinuousRoundedRectangle(cornerRadius.dp) // Dynamic Shape!
                        )
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousRoundedRectangle(cornerRadius.dp) },
                            effects = {
                                vibrancy()
                                blur(8.dp.toPx())
                                lens(12.dp.toPx(), 24.dp.toPx())
                            },
                            onDrawSurface = {
                                drawRect(Color.White.copy(0.15f))
                            }
                        )
                )
            }
        }

        // 3. Tab Content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // We expect the user to pass HydroBottomNavigationItem here,
            // but we can't easily intercept the clicks inside 'content'.
            // Instead, the user should call shockwaveState.trigger() in their onClick.
            content()
        }
    }
}


/**
 * A tab item that applies HydroChroma when selected AND triggers shockwave.
 */
@Composable
fun HydroBottomNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    shockwaveState: ShockwaveState? = null // 👈 Optional helper
) {
    val scale by animateFloatAsState(if (selected) 1.2f else 1.0f, label = "TabScale")
    val scope = rememberCoroutineScope()

    val chromaModifier = if (selected && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Modifier.hydroChroma(
            chromaticConfig = chromaticAberration(listOf(Color.Red, Color.Green, Color.Blue)),
            effects = effects(chromaticDensity = 1.5f, fluidity = 0f),
            isBackgroundLiquidGlass = true
        )
    } else {
        Modifier
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    onClick()
                    // 💥 Trigger Shockwave if provided!
                    if (!selected) { // Only trigger on change
                        scope.launch { shockwaveState?.trigger() }
                    }
                }
            )
            .padding(8.dp)
            .scale(scale)
            .then(chromaModifier)
    ) {
        Box(
            modifier = Modifier.size(24.dp).then(
                if (selected) Modifier else Modifier.alpha(0.5f)
            )
        ) {
            icon()
        }
        Box(Modifier.then(if (selected) Modifier else Modifier.alpha(0.5f))) {
            label()
        }
    }
}