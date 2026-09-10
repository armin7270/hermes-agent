package com.armin7270.snispoof.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armin7270.snispoof.state.AppSettings
import com.armin7270.snispoof.state.ConnectionState
import com.armin7270.snispoof.state.EngineStats
import com.armin7270.snispoof.state.PerAppMode
import com.armin7270.snispoof.state.VpnViewModel
import com.armin7270.snispoof.ui.theme.SpoofColors
import com.armin7270.snispoof.ui.theme.colorsFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vm: VpnViewModel,
    onOpenSettings: () -> Unit,
    onOpenApps: () -> Unit,
    onConnect: () -> Unit = {},
) {
    val state by vm.connectionState.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val accent = colorsFor(state).accent
    var showDnsSheet by remember { mutableStateOf(false) }

    ToolPageBackground(accent) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 14.dp),
        ) {
            // ---- top bar ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Rounded.Settings, null, tint = SpoofColors.TextPrimary)
                }
                AppTitle(accent)
                IconButton(onClick = onOpenApps) {
                    Icon(Icons.Rounded.Dns, null, tint = SpoofColors.TextPrimary)
                }
            }

            Spacer(Modifier.height(2.dp))
            StatusPill(state, accent)
            Spacer(Modifier.height(8.dp))

            // ---- big button ----
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ConnectButton(
                    state = state,
                    accent = accent,
                    onClick = {
                        when (state) {
                            ConnectionState.DISCONNECTED, ConnectionState.ERROR -> onConnect()
                            ConnectionState.CONNECTED -> vm.disconnect()
                            ConnectionState.CONNECTING, ConnectionState.DISCONNECTING -> Unit
                        }
                    },
                )
            }

            if (state == ConnectionState.ERROR) {
                val err by vm.errorMessage.collectAsStateWithLifecycle()
                err?.let {
                    Text(
                        it,
                        color = SpoofColors.ErrorRed,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ---- stats ----
            StatsGrid(stats, accent)

            Spacer(Modifier.height(10.dp))

            // ---- mode summary ----
            ModeSummaryCard(settings, accent, onOpenSettings, onOpenApps, { showDnsSheet = true })

            Spacer(Modifier.height(10.dp))

            // ---- live log console ----
            LogConsole(logs, accent, Modifier.weight(1f))
        }
    }

    if (showDnsSheet) {
        DnsBottomSheet(settings, accent, vm, { showDnsSheet = false })
    }
}

@Composable
private fun AppTitle(accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "SNI SPOOFING",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.55.sp,
            textAlign = TextAlign.Center,
            style = TextStyle(
                brush = Brush.horizontalGradient(
                    0f to accent,
                    0.30f to Color(0xFFDFF8FF),
                    0.58f to Color.White,
                    0.82f to Color(0xFFB8DFFF),
                    1f to accent,
                ),
            ),
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(132.dp)
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            accent.copy(alpha = 0.95f),
                            Color.White,
                            accent.copy(alpha = 0.95f),
                            Color.Transparent,
                        )
                    ),
                    RoundedCornerShape(50),
                ),
        )
    }
}

@Composable
private fun StatusPill(state: ConnectionState, accent: Color) {
    val label = when (state) {
        ConnectionState.DISCONNECTED -> t("Disconnected", "قطع است")
        ConnectionState.CONNECTING -> t("Connecting…", "در حال اتصال…")
        ConnectionState.CONNECTED -> t("Connected", "متصل")
        ConnectionState.DISCONNECTING -> t("Disconnecting…", "در حال قطع…")
        ConnectionState.ERROR -> t("Error", "خطا")
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(accent.copy(alpha = 0.10f), RoundedCornerShape(50))
                .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(accent, RoundedCornerShape(50))
            )
            Spacer(Modifier.size(8.dp))
            Text(label, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatsGrid(stats: EngineStats, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatChip(t("Uptime", "زمان"), formatUptime(stats.uptimeSec), accent)
        StatChip(t("Packets", "بسته‌ها"), stats.packetsInspected.toString(), accent)
        StatChip(t("Fragments", "قطعه‌بندی"), stats.fragmentsInjected.toString(), accent)
        StatChip(t("Flows", "اتصال‌ها"), stats.activeFlows.toString(), accent)
    }
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatChip("↑", formatBytes(stats.upBytes), accent)
        StatChip("↓", formatBytes(stats.downBytes), accent)
        StatChip(t("ClientHello", "ClientHello"), stats.clientHellosSeen.toString(), accent)
        StatChip(t("Dropped", "ریزش"), stats.packetsDropped.toString(), accent)
    }
}

@Composable
private fun ModeSummaryCard(
    settings: AppSettings,
    accent: Color,
    onOpenSettings: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenDns: () -> Unit,
) {
    ToolCard(
        modifier = Modifier.padding(horizontal = 18.dp),
        accent = accent,
    ) {
        SectionLabel(t("Active mode", "حالت فعال"))
        val perApp = when (settings.perAppMode) {
            PerAppMode.ALL -> t("All apps", "همه برنامه‌ها")
            PerAppMode.WHITELIST -> t("Whitelist (${settings.perAppPackages.size})", "لیست سفید (${settings.perAppPackages.size})")
            PerAppMode.BLACKLIST -> t("Blacklist (${settings.perAppPackages.size})", "لیست سیاه (${settings.perAppPackages.size})")
        }
        RowInfo(Icons.Rounded.Shield, t("Per-app routing", "مسیریابی برنامه‌ها"), perApp, onOpenApps)
        RowInfo(Icons.Rounded.Dns, "DNS", settings.dnsIp, onOpenDns)
        RowInfo(
            Icons.Rounded.Settings,
            t("More settings", "تنظیمات بیشتر"),
            t("Tap to open", "برای بازکردن بزنید"),
            onOpenSettings,
        )
    }
}

@Composable
private fun RowInfo(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Icon(icon, null, tint = SpoofColors.TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(10.dp))
        Text(label, color = SpoofColors.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = SpoofColors.TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.size(4.dp))
        Icon(
            Icons.Rounded.Menu,
            null,
            tint = Color.Transparent,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun LogConsole(logs: List<String>, accent: Color, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .background(Color(0xB307111D), RoundedCornerShape(16.dp))
            .border(1.dp, SpoofColors.CardBorder, RoundedCornerShape(16.dp)),
    ) {
        Text(
            t("Live log", "لاگ زنده"),
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 14.dp, top = 10.dp),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            items(logs) { line ->
                Text(
                    line,
                    color = SpoofColors.TextSecondary,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DnsBottomSheet(
    settings: AppSettings,
    accent: Color,
    vm: VpnViewModel,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SpoofColors.Surface,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            SectionLabel("DNS")
            Spacer(Modifier.height(8.dp))
            com.armin7270.snispoof.state.DnsProvider.entries.forEach { p ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            vm.setDnsProvider(p)
                            onDismiss()
                        }
                        .padding(vertical = 10.dp),
                ) {
                    Box(
                        Modifier
                            .size(14.dp)
                            .background(
                                if (settings.dnsProvider == p) accent else Color.Transparent,
                                RoundedCornerShape(50)
                            )
                            .border(
                                1.dp,
                                if (settings.dnsProvider == p) accent else SpoofColors.TextSecondary,
                                RoundedCornerShape(50)
                            )
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(p.label, color = SpoofColors.TextPrimary, fontSize = 15.sp)
                    Spacer(Modifier.weight(1f))
                    Text(p.primary, color = SpoofColors.TextSecondary, fontSize = 12.sp)
                }
            }
            if (settings.dnsProvider == com.armin7270.snispoof.state.DnsProvider.CUSTOM) {
                Spacer(Modifier.height(8.dp))
                TextFieldRow(
                    label = t("Custom DNS IP", "آی‌پی DNS سفارشی"),
                    value = settings.customDns,
                    accent = accent,
                    onCommit = { vm.setCustomDns(it) },
                    hint = "1.1.1.1",
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
