package com.arkstudios.hydrochroma

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Imports from your library
import com.example.hydrochroma.hydroChroma
// Imports from Kyant's libraries
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousRoundedRectangle
import com.arkstudios.hydrochroma.R
import com.example.hydrochroma.ChromaticConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF101010)), // Dark background
                contentAlignment = Alignment.Center
            ) {
                IntegratedDemo()
            }
        }
    }
}

@Composable
fun IntegratedDemo() {
    // 1. The Stage Setup
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505)),
        contentAlignment = Alignment.Center
    ) {

        // 2. The Recorder (Kyant's RenderNode Magic)
        val backdrop = rememberLayerBackdrop {
            drawContent()
        }

        // 3. The Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            PatternBackground()
        }

        // 4. The Hero Element
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Text(
                    "LIVING CRYSTAL",
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.sp,
                    modifier = Modifier.padding(bottom = 40.dp)
                )

                Box(
                    modifier = Modifier
                        .size(320.dp, 120.dp)
                        // LAYER 1: HydroChroma (Physics + Colors)
                        .hydroChroma(
                            chromaticConfig = null
                        )
                        // LAYER 2: LiquidGlass (Visuals)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousRoundedRectangle(48.dp) },
                            effects = {
                                vibrancy()
                                lens(
                                    refractionHeight = 16.dp.toPx(),
                                    refractionAmount = 32.dp.toPx(),
                                    chromaticAberration = false // Disable Kyant's static CA, use ours
                                )
                            },
                            onDrawSurface = {
                                // Glass sheen
                                drawRect(Color.White.copy(alpha = 0.08f))
                                // Border
                                drawRect(
                                    Color.White.copy(alpha = 0.1f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                )
                            }
                        )
                ) {
                    // Content Inside the Glass (Target for Chroma)
                    Text(
                        "    TOUCH    ",
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .hydroChroma(
                            // 🌈 Custom 5-Color Palette
                            chromaticConfig = ChromaticConfig(
                                colors = listOf(
                                    Color(0xFFFF0000), // Red
                                    Color(0xFF000000), // Black
                                    Color(0xFF00FF00), // Green

                                )
                            ),
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun PatternBackground() {
    Column {
        Image(
            painter = painterResource(id = R.drawable.img),
            contentDescription = null,
        )
    }
}