package com.armin7270.snispoof.ui

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armin7270.snispoof.state.VpnViewModel
import com.armin7270.snispoof.ui.theme.SpoofColors
import com.armin7270.snispoof.ui.theme.SpoofTheme

class MainActivity : ComponentActivity() {

    private val vm: VpnViewModel by viewModels { VpnViewModel.factory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            LaunchedEffect(settings.language) {
                L10n.language.value = settings.language
            }
            SpoofTheme {
                App()
            }
        }
    }

    @Composable
    private fun App() {
        val context = LocalContext.current
        var destination by remember { mutableStateOf(Destination.HOME) }

        val vpnLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) vm.connect()
        }
        val notifLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= 33) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val requestConnect: () -> Unit = {
            val prepare: Intent? = VpnService.prepare(context)
            if (prepare != null) vpnLauncher.launch(prepare) else vm.connect()
        }

        Scaffold(
            containerColor = SpoofColors.BackgroundTop,
            bottomBar = {
                NavigationBar(containerColor = SpoofColors.Surface) {
                    Destination.entries.forEach { dest ->
                        NavigationBarItem(
                            selected = destination == dest,
                            onClick = { destination = dest },
                            icon = { Icon(dest.icon, null) },
                            label = { Text(dest.label()) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (destination) {
                    Destination.HOME ->
                        MainScreen(vm, onOpenSettings = { destination = Destination.SETTINGS }, onOpenApps = { destination = Destination.APPS }, onConnect = requestConnect)
                    Destination.APPS ->
                        AppsScreen(vm) { destination = Destination.HOME }
                    Destination.SETTINGS ->
                        SettingsScreen(vm, onBack = { destination = Destination.HOME }, onOpenApps = { destination = Destination.APPS })
                }
            }
        }
    }

    private enum class Destination(val icon: ImageVector) {
        HOME(Icons.Rounded.Home),
        APPS(Icons.Rounded.Dns),
        SETTINGS(Icons.Rounded.Settings);

        @Composable
        fun label(): String = when (this) {
            HOME -> t("Home", "خانه")
            APPS -> t("Apps", "برنامه‌ها")
            SETTINGS -> t("Settings", "تنظیمات")
        }
    }
}
