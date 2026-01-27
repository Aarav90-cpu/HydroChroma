package com.arkstudios.hydrochroma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.arkstudios.hydrochroma.components.HydroBottomNavigation
import com.arkstudios.hydrochroma.components.HydroBottomNavigationItem

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
                HydroTabDemo()
            }
        }
    }


@Composable
fun HydroTabDemo() {
    // 1. Recorder (Background)
    val backdrop = HydroDrop(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.wallpaper_light), // Ensure you have this resource
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }

    // 2. State for Shockwave
    var selectedTab by remember { mutableIntStateOf(0) }
    val shockwaveProgress = remember { Animatable(0f) }

    // Trigger the shockwave whenever tab changes
    LaunchedEffect(selectedTab) {
        shockwaveProgress.snapTo(0f)
        shockwaveProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(600) // Fast, snappy glitch
        )
    }

    // 3. The Root Container (Gets the Shockwave!)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .hydroChroma(
                chromaticConfig = chromaticAberration(listOf(Color.Cyan, Color.Magenta, Color.Yellow)),
                animations = animations(onClick = HydroAction.Expand), // Using Expand logic
                effects = effects(
                    chromaticDensity = 1.5f, // Strong glitch
                    fluidity = 0.5f, // Fast shockwave
                    maxTilt = 0f // No tilt needed for this click effect
                ),
            )
    ) {
        // Page Content
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "PAGE ${selectedTab + 1}",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }

        // 4. The Liquid Bar
        HydroBottomNavigation(
            backdrop = backdrop,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            repeat(4) { index ->
                HydroBottomNavigationItem(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    icon = { Text("●", color = Color.White) }, // Replace with Icon
                    label = { Text("Tab ${index + 1}", color = Color.White, fontSize = 10.sp) }
                )
            }
        }
    }
}