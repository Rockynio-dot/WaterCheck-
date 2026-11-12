package app.watercheck.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { WaterTheme { App() } }
    }
}

private val Light = lightColorScheme(
    primary = Color(0xFF1976D2),
    primaryContainer = Color(0xFFD3E6FB),
    onPrimaryContainer = Color(0xFF0B2E55),
    secondaryContainer = Color(0xFFD3E6FB),
    onSecondaryContainer = Color(0xFF0B2E55),
    background = Color(0xFFF6F9FD),
    surface = Color(0xFFF6F9FD),
    surfaceVariant = Color(0xFFE3ECF6),
    surfaceContainer = Color(0xFFEAF1F9),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7DB9F5),
    onPrimary = Color(0xFF07305C),
    primaryContainer = Color(0xFF12447C),
    onPrimaryContainer = Color(0xFFD3E6FB),
    secondaryContainer = Color(0xFF12447C),
    onSecondaryContainer = Color(0xFFD3E6FB),
    background = Color(0xFF0E141B),
    surface = Color(0xFF0E141B),
    surfaceVariant = Color(0xFF1E2935),
    surfaceContainer = Color(0xFF17202A),
)

@Composable
fun WaterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}

@Composable
private fun App(vm: WaterViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val askNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Home, null) }, label = { Text("Dnes") },
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.DateRange, null) }, label = { Text("Historie") },
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Nastavení") },
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val s = state ?: return@Box
            when (tab) {
                0 -> HomeScreen(s, vm)
                1 -> HistoryScreen(s)
                else -> SettingsScreen(s.settings, vm)
            }
        }
    }
}
