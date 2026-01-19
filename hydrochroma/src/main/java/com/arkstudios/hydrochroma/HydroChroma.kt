package com.arkstudios.hydrochroma

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.random.Random

// --- 🌊 Configuration Classes ---

@Immutable
data class ChromaticConfig(
    val colors: List<Color> = listOf(Color.Red, Color.Green, Color.Blue)
)

fun chromaticAberration(colors: List<Color>): ChromaticConfig {
    return ChromaticConfig(colors.take(8))
}

@Immutable
data class ExpandConfig(
    val strength: ClosedRange<Dp> = 10.dp..35.dp,
    val width: ClosedRange<Dp> = 80.dp..200.dp
)

fun expand(
    strength: ClosedRange<Dp> = 10.dp..35.dp,
    width: ClosedRange<Dp> = 80.dp..200.dp
): ExpandConfig = ExpandConfig(strength, width)

@Immutable
data class HydroEffects(
    val blur: Dp = 0.dp,
    val vibrancy: Float = 1f,
    val chromaticDensity: Float = 1f,
    val fluidity: Float = 1f,
    val morphStrength: Float = 0.2f,
    val expand: ExpandConfig = ExpandConfig(),
    val maxTilt: Float = 10f
)

fun effects(
    blur: Dp = 0.dp,
    vibrancy: Float = 1f,
    chromaticDensity: Float = 1f,
    fluidity: Float = 1f,
    morphStrength: Float = 0.2f,
    expand: ExpandConfig = ExpandConfig(),
    maxTilt: Float = 10f
): HydroEffects = HydroEffects(blur, vibrancy, chromaticDensity, fluidity, morphStrength, expand, maxTilt)

enum class HydroAction {
    Expand, Fluid, Morph
}

@Immutable
data class HydroAnimations(
    val clickAction: HydroAction? = null,
    val animateAction: HydroAction? = null
)

fun animations(
    onClick: HydroAction? = null,
    onAnimate: HydroAction? = null
): HydroAnimations = HydroAnimations(onClick, onAnimate)

// --- 🧊 Liquid Glass Configuration ---

@Immutable
data class LiquidGlassConfig(
    val blurRadius: Dp = 8.dp,
    val refractionHeight: Dp = 12.dp,
    val refractionAmount: Dp = 24.dp,
    val vibrancy: Boolean = true,
    val depthEffect: Boolean = true,
    val chromaticAberration: Boolean = false // Standard static CA (disabled if using HydroChroma)
)

fun liquidGlassConfig(
    blurRadius: Dp = 8.dp,
    refractionHeight: Dp = 12.dp,
    refractionAmount: Dp = 24.dp,
    vibrancy: Boolean = true,
    depthEffect: Boolean = true,
    chromaticAberration: Boolean = false
) = LiquidGlassConfig(blurRadius, refractionHeight, refractionAmount, vibrancy, depthEffect, chromaticAberration)

// --- 💧 Helper Components ---

/**
 * A wrapper that records its content into a [Backdrop].
 * Use this to wrap the image/background you want to be "behind" the glass.
 */
@Composable
fun HydroDrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
): Backdrop {
    val backdrop = rememberLayerBackdrop {
        drawContent()
    }

    Box(
        modifier = modifier.layerBackdrop(backdrop),
        content = content
    )

    return backdrop
}

/**
 * 🌊 HydroGlass Container
 * A layout that applies Hydro Physics and Liquid Glass visuals to its content.
 * * @param draggable If true, the component can be dragged around.
 * @param onPositionChange Callback for when the drag position updates (delta).
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun HydroGlass(
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    shape: Shape = ContinuousCapsule,
    chromaticConfig: ChromaticConfig? = ChromaticConfig(),
    animations: HydroAnimations = animations(onClick = HydroAction.Fluid),
    effects: HydroEffects = effects(),
    liquidGlass: LiquidGlassConfig = liquidGlassConfig(),
    isBackgroundLiquidGlass: Boolean = true,
    draggable: Boolean = false,
    onPositionChange: ((Offset) -> Unit)? = null,
    // Custom Visual Overrides (Optional)
    highlight: (HydroState.() -> Highlight)? = null,
    shadow: (HydroState.() -> Shadow)? = null,
    innerShadow: (HydroState.() -> InnerShadow)? = null,
    onDrawSurface: (DrawScope.(HydroState) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberHydroState()
    val scope = rememberCoroutineScope()

    // Animation for interaction (Press/Drag)
    val interactionAnim = remember { Animatable(0f) }

    // Internal physics for the ripple effect
    val rippleProgress = remember { Animatable(0f) }

    // Drag offset state
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // 🌊 Physics Engine (Drag Detection)
    val physicsModifier = Modifier.pointerInput(Unit) {
        val velocityTracker = VelocityTracker()
        detectDragGestures(
            onDragStart = { offset ->
                state.touchPosition = offset
                scope.launch {
                    interactionAnim.animateTo(1f, spring(stiffness = Spring.StiffnessLow))
                }
            },
            onDragEnd = {
                scope.launch {
                    interactionAnim.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
                }

                // 🌊 Ripple when it stops dragging!
                scope.launch {
                    rippleProgress.snapTo(0f)
                    rippleProgress.animateTo(1f, tween(1000, easing = LinearEasing))
                }

                state.velocity = 0f
                state.scaleX = 1f
                state.scaleY = 1f
            },
            onDragCancel = {
                scope.launch { interactionAnim.animateTo(0f) }
            }
        ) { change, dragAmount ->
            change.consume()
            velocityTracker.addPosition(change.uptimeMillis, change.position)
            state.touchPosition = change.position

            // Handle Dragging
            if (draggable) {
                dragOffset += dragAmount
                onPositionChange?.invoke(dragAmount)
            }

            // Calculate velocity for stretching
            val velocity = velocityTracker.calculateVelocity()
            val velocityMag = (velocity.x.absoluteValue + velocity.y.absoluteValue) / 2000f // Normalize
            state.velocity = velocityMag

            // Stretch logic
            state.scaleX = 1f / (1f - (velocityMag * 0.1f).fastCoerceIn(-0.2f, 0.2f))
            state.scaleY = 1f * (1f - (velocityMag * 0.05f).fastCoerceIn(-0.2f, 0.2f))
        }
    }.pointerInput(Unit) {
        // 🌊 Detect Taps for clicking!
        detectTapGestures(
            onPress = { offset ->
                state.touchPosition = offset
                scope.launch {
                    interactionAnim.animateTo(0.2f, spring(stiffness = Spring.StiffnessMedium))
                }
                tryAwaitRelease()
                scope.launch {
                    interactionAnim.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                }
            },
            onTap = {
                // 🌊 Ripple when tapped!
                scope.launch {
                    rippleProgress.snapTo(0f)
                    rippleProgress.animateTo(1f, tween(1000, easing = LinearEasing))
                }
            }
        )
    }

    // Sync state
    state.interactionProgress = interactionAnim.value

    // Apply HydroChroma (The Distortion)
    Box(
        modifier = modifier
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) } // Apply Drag Position
            .then(physicsModifier)
            .hydroChroma(
                chromaticConfig = chromaticConfig,
                animations = animations, // Ripples handled manually via rippleProgress
                effects = effects,
                isBackgroundLiquidGlass = isBackgroundLiquidGlass,
                shape = shape,
                // We override the internal progress with our physics ripple
                manualRippleProgress = rippleProgress.value,
                manualTouchPosition = state.touchPosition
            )
            // Apply LiquidGlass (The Visuals)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    // Combine config from LiquidGlassConfig
                    if (liquidGlass.vibrancy) vibrancy()

                    // Dynamic Blur based on interaction + Config
                    val baseBlur = liquidGlass.blurRadius.toPx()
                    val dynamicBlur = baseBlur + (state.interactionProgress * 4f)
                    blur(dynamicBlur)

                    lens(
                        liquidGlass.refractionHeight.toPx(),
                        liquidGlass.refractionAmount.toPx(),
                        liquidGlass.depthEffect,
                        liquidGlass.chromaticAberration
                    )
                },
                highlight = {
                    highlight?.invoke(state) ?: Highlight.Default.copy(alpha = state.interactionProgress)
                },
                shadow = {
                    shadow?.invoke(state) ?: Shadow(alpha = state.interactionProgress)
                },
                innerShadow = {
                    innerShadow?.invoke(state) ?: InnerShadow(radius = 8.dp * state.interactionProgress, alpha = state.interactionProgress)
                },
                layerBlock = {
                    scaleX = state.scaleX
                    scaleY = state.scaleY
                },
                onDrawSurface = {
                    if (onDrawSurface != null) {
                        onDrawSurface(state)
                    } else {
                        // Default surface logic
                        val progress = state.interactionProgress
                        drawRect(Color.White.copy(0.1f), alpha = 1f - progress)
                        drawRect(Color.Black.copy(alpha = 0.03f * progress))
                    }
                }
            ),
        content = content
    )
}

// --- 🧠 Hydro Physics State ---

@Stable
class HydroState {
    var interactionProgress by mutableFloatStateOf(0f)
    var scaleX by mutableFloatStateOf(1f)
    var scaleY by mutableFloatStateOf(1f)
    var velocity by mutableFloatStateOf(0f)
    var touchPosition by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberHydroState(): HydroState {
    return remember { HydroState() }
}

// --- 📜 The Ultimate Hydro Shader (AGSL) ---
private const val HYDRO_CHROMA_SHADER = """
    uniform shader uContent;
    uniform float2 uCenter;
    uniform float uProgress;
    uniform float uTime;
    uniform float2 uSize;
    uniform float uSeed;
    uniform float uStrength;
    uniform float uWidth;
    uniform float uFluidity;
    uniform float uMorphStrength;
    uniform int uModeClick;
    uniform int uModeAnimate;
    uniform float2 uTilt;
    uniform int uColorCount;
    uniform half4 uColors[8];
    uniform int uUseAlphaMask;
    uniform float uChromaticDensity;

    float random(float2 st) {
        return fract(sin(dot(st.xy, float2(12.9898,78.233))) * 43758.5453123);
    }

    half4 main(float2 coord) {
        float2 totalDisplacement = float2(0.0);
        
        if (uModeAnimate == 1) { 
            float t = uTime * (uFluidity * 0.5); 
            float noiseX = sin(coord.y * 0.02 + t + uSeed) * cos(coord.x * 0.01 + t);
            float noiseY = cos(coord.x * 0.02 + t - uSeed) * sin(coord.y * 0.01 - t);
            totalDisplacement += float2(noiseX, noiseY) * (uMorphStrength * 10.0); 
        }

        if (uProgress > 0.0 && uProgress < 1.0) {
            float dist = distance(coord, uCenter);
            float maxRadius = length(uSize);
            
            if (uModeClick == 1) { 
                float currentRadius = maxRadius * uProgress;
                float diff = dist - currentRadius;
                if (abs(diff) < uWidth) {
                    float angle = (diff / uWidth) * 3.14159;
                    float wave = sin(angle) * uStrength * (1.0 - uProgress);
                    totalDisplacement += normalize(coord - uCenter) * wave;
                }
            } 
            else if (uModeClick == 2) { 
                float decay = smoothstep(maxRadius, 0.0, dist) * (1.0 - uProgress);
                float wave = sin(dist * 0.05 - (uProgress * 15.0 * uFluidity) + uSeed * 10.0);
                totalDisplacement += normalize(coord - uCenter) * wave * uStrength * decay;
            }
        }

        half4 finalColor = half4(0.0);
        half4 centerSample = uContent.eval(coord - totalDisplacement);
        
        float maskMultiplier = 1.0;
        if (uUseAlphaMask == 1) {
            maskMultiplier = smoothstep(0.1, 0.8, centerSample.a);
        }
        
        if (length(uTilt) < 0.001 || uColorCount == 0) {
            return centerSample;
        }

        float totalWeight = 0.0;
        for (int i = 0; i < 8; i++) {
            if (i >= uColorCount) break;
            float factor = float(i) / float(uColorCount - 1); 
            float offsetFactor = (factor - 0.5) * 2.0; 
            float2 colorOffset = (uTilt * 4.0 + totalDisplacement * 0.2) * offsetFactor * maskMultiplier * uChromaticDensity;
            half4 sample = uContent.eval(coord - totalDisplacement + colorOffset);
            finalColor += sample * uColors[i];
            totalWeight += 1.0;
        }
        
        if (totalWeight > 0.0) {
            finalColor = finalColor / (totalWeight * 0.45); 
        } else {
            finalColor = centerSample;
        }
        finalColor.a = centerSample.a;
        return finalColor;
    }
"""

/**
 * 🌊 Core Modifier.hydroChroma (Internal Engine)
 * Now supports manual control for the wrapper.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.hydroChroma(
    chromaticConfig: ChromaticConfig? = ChromaticConfig(),
    animations: HydroAnimations = animations(onClick = HydroAction.Fluid),
    effects: HydroEffects = effects(),
    isBackgroundLiquidGlass: Boolean = false,
    shape: Shape = RectangleShape,
    manualRippleProgress: Float? = null,
    manualTouchPosition: Offset? = null
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val clickProgress = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "HydroTime")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(100000, easing = LinearEasing), RepeatMode.Restart),
        label = "Time"
    )

    val internalTouchPosition = remember { mutableStateOf(Offset.Zero) }
    val viewSize = remember { mutableStateOf(Offset.Zero) }

    val currentSeed = remember { mutableFloatStateOf(Random.nextFloat() * 100f) }
    val currentStrength = remember { mutableFloatStateOf(15f) }
    val currentWidth = remember { mutableFloatStateOf(100f) }

    val tilt = rememberTilt()
    val shader = remember { RuntimeShader(HYDRO_CHROMA_SHADER) }

    val activeProgress = manualRippleProgress ?: clickProgress.value
    val activeTouch = manualTouchPosition ?: internalTouchPosition.value

    this
        .onSizeChanged { size ->
            viewSize.value = Offset(size.width.toFloat(), size.height.toFloat())
        }
        .then(
            if (manualRippleProgress == null) {
                Modifier.pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        internalTouchPosition.value = down.position
                        currentSeed.floatValue = Random.nextFloat() * 100f

                        val minStrength = with(density) { effects.expand.strength.start.toPx() }
                        val maxStrength = with(density) { effects.expand.strength.endInclusive.toPx() }
                        currentStrength.floatValue = Random.nextDouble(minStrength.toDouble(), maxStrength.toDouble()).toFloat()

                        val randomDuration = Random.nextLong(800, 1600)

                        if (animations.clickAction != null) {
                            scope.launch {
                                clickProgress.snapTo(0f)
                                clickProgress.animateTo(1f, tween(randomDuration.toInt(), easing = LinearEasing))
                            }
                        }
                    }
                }
            } else Modifier
        )
        .graphicsLayer {
            this.shape = shape
            this.clip = true

            shader.setFloatUniform("uCenter", activeTouch.x, activeTouch.y)
            shader.setFloatUniform("uProgress", activeProgress)
            shader.setFloatUniform("uTime", time)
            shader.setFloatUniform("uSize", viewSize.value.x, viewSize.value.y)

            shader.setFloatUniform("uSeed", currentSeed.floatValue)
            shader.setFloatUniform("uStrength", currentStrength.floatValue)
            shader.setFloatUniform("uWidth", currentWidth.floatValue)
            shader.setFloatUniform("uFluidity", effects.fluidity)
            shader.setFloatUniform("uMorphStrength", effects.morphStrength)

            val clickMode = when (animations.clickAction) {
                HydroAction.Expand -> 1; HydroAction.Fluid -> 2; else -> 0
            }
            val animateMode = when (animations.animateAction) {
                HydroAction.Morph -> 1; else -> 0
            }
            shader.setIntUniform("uModeClick", clickMode)
            shader.setIntUniform("uModeAnimate", animateMode)

            if (chromaticConfig != null) {
                val clampedTiltX = (tilt.x * 15f).coerceIn(-effects.maxTilt, effects.maxTilt)
                val clampedTiltY = (tilt.y * 15f).coerceIn(-effects.maxTilt, effects.maxTilt)
                shader.setFloatUniform("uTilt", clampedTiltX, clampedTiltY)
                shader.setFloatUniform("uChromaticDensity", effects.chromaticDensity)
                shader.setIntUniform("uColorCount", minOf(chromaticConfig.colors.size, 8))

                val colorArray = FloatArray(32)
                chromaticConfig.colors.take(8).forEachIndexed { index, color ->
                    colorArray[index * 4] = color.red
                    colorArray[index * 4 + 1] = color.green
                    colorArray[index * 4 + 2] = color.blue
                    colorArray[index * 4 + 3] = color.alpha
                }
                shader.setFloatUniform("uColors", colorArray)
                shader.setIntUniform("uUseAlphaMask", if (isBackgroundLiquidGlass) 1 else 0)
            } else {
                shader.setFloatUniform("uTilt", 0f, 0f)
                shader.setFloatUniform("uChromaticDensity", 0f)
                shader.setIntUniform("uColorCount", 0)
                shader.setIntUniform("uUseAlphaMask", 0)
            }

            var currentEffect = RenderEffect.createRuntimeShaderEffect(shader, "uContent")

            if (effects.blur > 0.dp) {
                val blurPx = effects.blur.toPx()
                val blurEffect = RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.MIRROR)
                currentEffect = RenderEffect.createChainEffect(currentEffect, blurEffect)
            }

            if (effects.vibrancy != 1f) {
                val matrix = ColorMatrix()
                matrix.setSaturation(effects.vibrancy)
                val vibranceEffect = RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix))
                currentEffect = RenderEffect.createChainEffect(currentEffect, vibranceEffect)
            }

            renderEffect = currentEffect.asComposeRenderEffect()
        }
}