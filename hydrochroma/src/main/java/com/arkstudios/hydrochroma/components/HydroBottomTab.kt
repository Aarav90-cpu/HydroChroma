package com.arkstudios.hydrochroma.components

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.arkstudios.hydrochroma.HydroAction
import com.arkstudios.hydrochroma.HydroChroma
import com.arkstudios.hydrochroma.animations
import com.arkstudios.hydrochroma.chromaticAberration
import com.arkstudios.hydrochroma.effects
import com.arkstudios.hydrochroma.hydroChroma
import com.arkstudios.hydrochroma.liquidGlassConfig
import com.arkstudios.hydrochroma.noLiquidGlass
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.capsule.ContinuousCapsule

/**
 * 🌊 HydroBottomNavigation
 *
 * A static liquid glass bottom bar.
 * When items are clicked, the parent is expected to handle the screen-wide shockwave.
 */
@Composable
fun HydroBottomNavigation(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()
    val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f) else Color(0xFF121212).copy(0.4f)

    Box(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(80.dp)
    ) {
        // The Liquid Glass Bar 🧊
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.Center)
            ) {
                HydroChroma(
                    backdrop = backdrop,
                    shape = ContinuousCapsule,
                    isBackgroundLiquidGlass = true,
                    chromaticConfig = null,
                    // Subtle fluid physics for the bar itself (breathing)
                    animations = animations(
                        onAnimate = HydroAction.Morph,
                        onClick = HydroAction.Fluid
                    ),
                    effects = effects(
                        fluidity = 1.2f,
                        morphStrength = 0.05f,
                        blur = 0.dp
                    ),
                    // Clean glass visuals
                    liquidGlass = noLiquidGlass(),
                    draggable = true,

                    onDrawSurface = {
                        // Glass tint
                        drawRect(containerColor)
                        // Subtle border
                        drawRect(
                            Color.White.copy(0.1f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(64.dp).align(Alignment.Center),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        content = content
                    )
                }
            }
        } else {
            // Fallback for older devices (Just the row)
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 * A simple item for the bar.
 * Scales up and applies Chromatic Aberration when selected.
 */
@Composable
fun HydroBottomNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit
) {
    // 1. Scale Animation: Pop up when selected!
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.25f else 0.9f,
        label = "TabScale"
    )

    // 2. Chromatic Modifier: Only apply RGB split if selected
    val chromaModifier = if (selected && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Modifier.hydroChroma(
            chromaticConfig = chromaticAberration(listOf(Color.Red, Color.Green, Color.Blue)),
            effects = effects(
                chromaticDensity = 0.5f, // Visible glitch
                fluidity = 0f // No ripples needed on the icon itself, just the color split
            ),
            isBackgroundLiquidGlass = true // Overlaying glass

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
                indication = null, // No ripple here, handled by parent/scale
                onClick = onClick
            )
            .padding(8.dp)
            .scale(scale) // 👈 Apply Scale
            .then(chromaModifier) // 👈 Apply Chromatic Aberration
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .then(
                    if (selected) Modifier
                    else Modifier.alpha(0.5f) // Fade out unselected
                )
        ) {
            icon()
        }

        Box(Modifier.then(if (selected) Modifier else Modifier.alpha(0.5f))) {
            label()
        }
    }
}