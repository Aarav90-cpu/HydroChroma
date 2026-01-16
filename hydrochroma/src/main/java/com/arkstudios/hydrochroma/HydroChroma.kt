package com.example.hydrochroma

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import com.arkstudios.hydrochroma.rememberTilt
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 🎨 Configuration for the Chromatic Aberration effect.
 * @param colors A list of colors to use for the spectral split. Max 8 colors.
 * Defaults to Red, Green, Blue if empty.
 */
@Immutable
data class ChromaticConfig(
    val colors: List<Color> = listOf(Color.Red, Color.Green, Color.Blue)
)

// 📜 The AGSL Shader Logic
// Supports up to 8 spectral colors and Alpha-Masking for Liquid Glass compatibility.
private const val HYDRO_CHROMA_SHADER = """
    uniform shader uContent;
    uniform float2 uCenter;     // Touch X,Y
    uniform float uProgress;    // Animation 0.0 -> 1.0
    uniform float2 uSize;       // View W,H
    uniform float uStrength;    // Randomized Ripple Intensity
    uniform float uWidth;       // Randomized Ripple Thickness
    
    // Chroma Uniforms
    uniform float2 uTilt;       // Gyro X,Y
    uniform int uColorCount;    // How many colors in the array?
    uniform half4 uColors[8];   // The color palette
    uniform int uUseAlphaMask;  // Boolean flag (1 = true, 0 = false)

    half4 main(float2 coord) {
        // --- 1. RIPPLE PHYSICS ---
        float dist = distance(coord, uCenter);
        float maxRadius = length(uSize);
        float currentRadius = maxRadius * uProgress;
        float diff = dist - currentRadius;
        
        float2 rippleOffset = float2(0.0, 0.0);
        
        // Smoothstep for softer ripple edges
        if (abs(diff) < uWidth) {
            float angle = (diff / uWidth) * 3.14159;
            // Damping: Wave gets weaker as it spreads
            float displacement = sin(angle) * uStrength * (1.0 - uProgress);
            float2 direction = normalize(coord - uCenter);
            rippleOffset = direction * displacement;
        }

        // --- 2. CHROMATIC ABERRATION ---
        
        half4 finalColor = half4(0.0);
        
        // Initial sample to determine Alpha for masking
        half4 centerSample = uContent.eval(coord - rippleOffset);
        
        // Smart Masking:
        // If isBackgroundLiquidGlass is ON (1), we scale the tilt effect by the alpha.
        // Opaque pixels (Text) -> Multiplier 1.0 (Full Effect)
        // Transparent pixels (Glass) -> Multiplier ~0.1 (Tiny Effect)
        float maskMultiplier = 1.0;
        if (uUseAlphaMask == 1) {
            maskMultiplier = smoothstep(0.1, 0.8, centerSample.a);
        }
        
        // If tilt is near zero or config is null, just return the ripple result
        if (length(uTilt) < 0.001 || uColorCount == 0) {
            return centerSample;
        }

        float totalWeight = 0.0;
        
        // Loop through the custom colors
        // We spread the samples out based on the index
        for (int i = 0; i < 8; i++) {
            if (i >= uColorCount) break;
            
            // Map index -1 to 1 based on count
            float factor = float(i) / float(uColorCount - 1); // 0.0 to 1.0
            float offsetFactor = (factor - 0.5) * 2.0; // -1.0 to 1.0
            
            // Calculate offset for this specific color "ghost"
            // Apply maskMultiplier here to restrict it to content!
            float2 colorOffset = uTilt * offsetFactor * 4.0 * maskMultiplier;
            
            // Sample the image at the offset
            half4 sample = uContent.eval(coord - rippleOffset + colorOffset);
            
            // Add tint: We take the sample's luminance and apply the custom color
            // This creates a "Spectral Ghosting" effect
            float luminance = dot(sample.rgb, float3(0.299, 0.587, 0.114));
            
            // Blending mode: Additive tinting
            finalColor += sample * uColors[i];
            totalWeight += 1.0;
        }
        
        // Normalize
        if (totalWeight > 0.0) {
            finalColor = finalColor / (totalWeight * 0.4); // 0.4 keeps it bright
        } else {
            finalColor = centerSample;
        }
        
        // Restore original alpha to keep edges clean
        finalColor.a = centerSample.a;
        
        return finalColor;
    }
"""

/**
 * 🌊 Modifier.hydroChroma
 *
 * @param chromaticConfig Configuration for custom spectral colors. Pass null to disable CA.
 * @param isBackgroundLiquidGlass If true, prevents CA from distorting the semi-transparent glass background.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.hydroChroma(
    chromaticConfig: ChromaticConfig? = ChromaticConfig(),
    isBackgroundLiquidGlass: Boolean = false
): Modifier = composed {
    val scope = rememberCoroutineScope()

    // Animation State
    val rippleProgress = remember { Animatable(0f) }

    // Touch & View State
    val touchPosition = remember { mutableStateOf(Offset.Zero) }
    val viewSize = remember { mutableStateOf(Offset.Zero) }

    // Randomness State
    val currentStrength = remember { mutableFloatStateOf(15f) }
    val currentWidth = remember { mutableFloatStateOf(100f) }

    // Sensor State
    val tilt = rememberTilt()

    // Shader State
    val shader = remember { RuntimeShader(HYDRO_CHROMA_SHADER) }

    this
        .onSizeChanged { size ->
            viewSize.value = Offset(size.width.toFloat(), size.height.toFloat())
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown()
                touchPosition.value = down.position

                // Randomize parameters
                currentStrength.floatValue = Random.nextDouble(10.0, 35.0).toFloat()
                currentWidth.floatValue = Random.nextDouble(80.0, 200.0).toFloat()
                val randomDuration = Random.nextLong(1000, 1800)

                scope.launch {
                    rippleProgress.snapTo(0f)
                    rippleProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = randomDuration.toInt(),
                            easing = LinearEasing
                        )
                    )
                }
            }
        }
        .graphicsLayer {
            // Ripple Uniforms
            shader.setFloatUniform("uCenter", touchPosition.value.x, touchPosition.value.y)
            shader.setFloatUniform("uProgress", rippleProgress.value)
            shader.setFloatUniform("uSize", viewSize.value.x, viewSize.value.y)
            shader.setFloatUniform("uStrength", currentStrength.floatValue)
            shader.setFloatUniform("uWidth", currentWidth.floatValue)

            // Tilt Uniforms
            if (chromaticConfig != null) {
                shader.setFloatUniform("uTilt", tilt.x * 15f, tilt.y * 15f)
                shader.setIntUniform("uColorCount", minOf(chromaticConfig.colors.size, 8))

                // Flatten colors for AGSL array
                val colorArray = FloatArray(32) // 8 colors * 4 components (RGBA)
                chromaticConfig.colors.take(8).forEachIndexed { index, color ->
                    colorArray[index * 4] = color.red
                    colorArray[index * 4 + 1] = color.green
                    colorArray[index * 4 + 2] = color.blue
                    colorArray[index * 4 + 3] = color.alpha
                }
                shader.setFloatUniform("uColors", colorArray)
                shader.setIntUniform("uUseAlphaMask", if (isBackgroundLiquidGlass) 1 else 0)
            } else {
                // Disable Chroma if config is null
                shader.setFloatUniform("uTilt", 0f, 0f)
                shader.setIntUniform("uColorCount", 0)
            }

            renderEffect = android.graphics.RenderEffect
                .createRuntimeShaderEffect(shader, "uContent")
                .asComposeRenderEffect()
            clip = true
        }
}