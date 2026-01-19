package com.arkstudios.hydrochroma

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousRoundedRectangle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                HydroGlassDemo()
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Requires Android 13+", color = Color.White)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun HydroGlassDemo() {
    // 1. The Stage: Define the background to be refracted
    val backdrop = HydroDrop(Modifier.fillMaxSize()) {
        // A trippy gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()

        ) {
            // Add some stripes for extra refraction detail
            Column(Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(R.drawable.wallpaper_light),
                    contentDescription = null,
                )
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Text(
            "DRAG & TOUCH",
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp),
            color = Color.White.copy(0.5f),
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )

        // 2. The Hero: HydroGlass Component
        HydroGlass(
            modifier = Modifier
                .align(Alignment.Center)
                .width(300.dp)
                .height(200.dp),

            // Connect to our background
            backdrop = backdrop,

            // Shape
            shape = ContinuousRoundedRectangle(48.dp),

            // 🌊 Physics Configuration
            animations = animations(
                onClick = HydroAction.Fluid, // Splash!
                onAnimate = HydroAction.Morph, // Breathe...
            ),
            effects = effects(
                fluidity = 5f,         // Very liquid
                chromaticDensity = 0.8f, // Strong color split
                maxTilt = 5f            // Cap tilt distortion
            ),
            chromaticConfig = chromaticAberration(
                listOf(Color.Cyan, Color.Magenta, Color.Yellow)
            ),

            // 🧊 Visuals Configuration
            liquidGlass = liquidGlassConfig(
                blurRadius = 1.dp,
                refractionHeight = 64.dp,
                refractionAmount = 128.dp,
                vibrancy = false,
                chromaticAberration = false // Let Hydro handle the chroma
            ),

            // Behavior
            draggable = true,
            isBackgroundLiquidGlass = true,

            // Custom Drawing
        ) {
            // Content inside the glass
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Hydro Glass",
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    modifier = Modifier
                        .hydroChroma()
                )
            }
        }
    }
}