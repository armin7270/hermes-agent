package com.armin7270.snispoof.ui

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armin7270.snispoof.state.VpnViewModel
import com.armin7270.snispoof.ui.theme.SpoofColors

/** Per-app whitelist / blacklist picker (UAC-style app bypass screen). */
@Composable
fun AppsScreen(vm: VpnViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val accent = SpoofColors.DisconnectedBlue
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    val apps = remember {
        context.packageManager.getInstalledApplications(0)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .filter { context.packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .map { AppEntry(it.packageName, context.packageManager.getApplicationLabel(it).toString()) }
            .sortedBy { it.label.lowercase() }
    }

    ToolPageBackground(accent) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = SpoofColors.TextPrimary)
                }
                Column {
                    Text(t("Per-app routing", "مسیریابی برنامه‌ها"), color = SpoofColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        when (settings.perAppMode) {
                            com.armin7270.snispoof.state.PerAppMode.ALL -> t("Mode: all apps", "حالت: همه برنامه‌ها")
                            com.armin7270.snispoof.state.PerAppMode.WHITELIST -> t("Mode: whitelist", "حالت: لیست سفید")
                            com.armin7270.snispoof.state.PerAppMode.BLACKLIST -> t("Mode: blacklist", "حالت: لیست سیاه")
                        },
                        color = SpoofColors.TextSecondary, fontSize = 11.sp,
                    )
                }
            }
            TextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(t("Search apps…", "جستجوی برنامه‌ها…"), color = SpoofColors.TextSecondary) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SpoofColors.ButtonCenter,
                    unfocusedContainerColor = SpoofColors.ButtonEdge,
                    cursorColor = accent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
            )
            Spacer(Modifier.size(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val filtered = apps.filter {
                    query.isBlank() || it.label.contains(query, true) || it.pkg.contains(query, true)
                }
                items(filtered, key = { it.pkg }) { app ->
                    val selected = app.pkg in settings.perAppPackages
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.togglePerAppPackage(app.pkg) }
                            .background(
                                if (selected) accent.copy(alpha = 0.08f) else Color.Transparent,
                                RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        val icon = remember(app.pkg) {
                            runCatching {
                                context.packageManager.getApplicationIcon(app.pkg)
                            }.getOrNull()
                        }
                        Box(
                            Modifier
                                .size(38.dp)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            icon?.let {
                                val bmp = remember(app.pkg) {
                                    runCatching { it.toBitmap().asImageBitmap() }.getOrNull()
                                }
                                bmp?.let { Image(it, null, modifier = Modifier.size(26.dp)) }
                            }
                        }
                        Spacer(Modifier.size(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, color = SpoofColors.TextPrimary, fontSize = 14.sp, maxLines = 1)
                            Text(app.pkg, color = SpoofColors.TextSecondary, fontSize = 10.sp, maxLines = 1)
                        }
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { vm.togglePerAppPackage(app.pkg) },
                            colors = CheckboxDefaults.colors(checkedColor = accent),
                        )
                    }
                }
            }
        }
    }
}

private data class AppEntry(val pkg: String, val label: String)

private fun android.graphics.drawable.Drawable.toBitmap(): android.graphics.Bitmap {
    val bmp = android.graphics.Bitmap.createBitmap(intrinsicWidth.coerceAtLeast(1), intrinsicHeight.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bmp
}
