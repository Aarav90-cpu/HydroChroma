package com.kyant.backdrop.catalog

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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkstudios.hydrochroma.HydroDrop
import com.arkstudios.hydrochroma.R
import kotlinx.coroutines.launch
import com.arkstudios.hydrochroma.*
import com.arkstudios.hydrochroma.components.HydroBottomNavigation
import com.arkstudios.hydrochroma.components.HydroBottomNavigationItem

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
                HydroNavigationDemo()

        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun HydroNavigationDemo() {
    // 1. The Stage: Capture the background for glass refraction

        // A rich background to show off the glass and chromatic effects
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Use a wallpaper if available, otherwise a generative pattern
            Image(
                painter = painterResource(R.drawable.system_home_screen_light), // Make sure you have this drawable!
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

    // 2. State Management
    var selectedTab by remember { mutableIntStateOf(0) }
    val shockwaveState = rememberShockwaveState() // 💥 THE SHOCKWAVE STATE
    val scope = rememberCoroutineScope()

    // 3. The Root Container (Wrapped in Shockwave!)
    ShockwaveContainer(state = shockwaveState) {

        // --- Page Content ---
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val title = when (selectedTab) {
                0 -> "HOME"
                1 -> "SEARCH"
                2 -> "FLUID"
                3 -> "PROFILE"
                else -> "HYDRO"
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 8.sp
                )
                Text(
                    text = "TAB ${selectedTab + 1}",
                    fontSize = 16.sp,
                    color = Color.White.copy(0.5f),
                    letterSpacing = 4.sp
                )
            }
        }

        // --- The Liquid Bar ---
        HydroBottomNavigation(
            selectedTabIndex = selectedTab,
            onTabSelected = { index ->
                if (selectedTab != index) {
                    selectedTab = index
                    // 💥 TRIGGER THE SHOCKWAVE ON TAB SWITCH
                    scope.launch { shockwaveState.trigger() }
                }
            },
            tabsCount = 4,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            repeat(4) { index ->
                HydroBottomNavigationItem(
                    selected = selectedTab == index,
                    onClick = {
                        if (selectedTab != index) {
                            selectedTab = index
                            // You can also trigger here if you want per-item logic
                            scope.launch { shockwaveState.trigger() }
                        }
                    },
                    icon = {
                        // Simple indicator for now, replace with Icons
                        Text(
                            text = if (selectedTab == index) "●" else "○",
                            color = Color.White,
                            fontSize = 24.sp
                        )
                    },
                    label = {
                        Text(
                            text = "Tab ${index + 1}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }
    }
}

