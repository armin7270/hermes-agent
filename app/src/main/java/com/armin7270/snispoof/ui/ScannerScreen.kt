package com.armin7270.snispoof.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armin7270.snispoof.core.scanner.Scanner
import com.armin7270.snispoof.state.VpnStateStore
import com.armin7270.snispoof.state.VpnViewModel
import com.armin7270.snispoof.ui.theme.SpoofColors
import kotlinx.coroutines.launch

/** Clean Cloudflare edge scanner + fake SNI prober (UAC-style). */
@Composable
internal fun ScannerScreen(vm: VpnViewModel, onMenuClick: () -> Unit) {
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val selectedId by vm.selectedProfileId.collectAsStateWithLifecycle()
    val accent = SpoofColors.ConnectingCyan
    val scope = rememberCoroutineScope()
    var scanning by remember { mutableStateOf<String?>(null) }
    var ipResults by remember { mutableStateOf<List<Scanner.IpResult>>(emptyList()) }
    var sniResults by remember { mutableStateOf<List<Scanner.SniResult>>(emptyList()) }

    val activeProfile = profiles.firstOrNull { it.id == selectedId }
    val fakeSni = activeProfile?.fakeSni ?: "auth.vercel.com"
    val targetEdge = activeProfile?.connectIp ?: "188.114.98.0"

    ToolPageScaffold(
        accent = accent,
        header = {
            ToolPageHeader(
                title = t("Scanner", "اسکنر"),
                subtitle = t("Clean Cloudflare IPs & fake SNIs", "آی‌پی تمیز کلودفلر و SNI جعلی"),
                icon = Icons.Rounded.Radar,
                accent = accent,
                onMenuClick = onMenuClick,
            )
        },
    ) {
        item {
            ToolCard(accent = accent) {
                SectionLabel(t("Clean Cloudflare IPs", "آی‌پی‌های تمیز کلودفلر"))
                Text(
                    t("Probes every known edge with a real TLS handshake and ranks them by latency.",
                        "هر edge شناخته‌شده را با هندشیک TLS واقعی می‌سنجد و بر اساس تاخیر مرتب می‌کند."),
                    color = SpoofColors.TextSecondary, fontSize = 11.5.sp,
                )
                Spacer(Modifier.height(8.dp))
                ActionButton(
                    label = if (scanning == "ip") t("Scanning…", "در حال اسکن…") else t("Scan IPs", "اسکن آی‌پی‌ها"),
                    accent = accent,
                    enabled = scanning == null,
                ) {
                    scanning = "ip"
                    scope.launch {
                        val results = Scanner.scanIps(
                            ips = Scanner.CLOUDFLARE_EDGES,
                            protector = null,
                            sni = fakeSni,
                        )
                        ipResults = results
                        results.firstOrNull()?.let { best ->
                            vm.profileStore.selected()?.let { p ->
                                vm.saveProfile(p.copy(connectIp = best.ip))
                            }
                            VpnStateStore.log("scanner: best edge ${best.ip} (${best.latencyMs}ms)")
                        }
                        scanning = null
                    }
                }
                if (ipResults.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        t("Tap an IP to use it as the edge", "روی آی‌پی بزنید تا به‌عنوان edge استفاده شود"),
                        color = SpoofColors.TextSecondary, fontSize = 11.sp,
                    )
                    ipResults.take(12).forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable {
                                    vm.profileStore.selected()?.let { p ->
                                        vm.saveProfile(p.copy(connectIp = r.ip))
                                    }
                                },
                        ) {
                            Text(r.ip, color = SpoofColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text(
                                "${r.latencyMs} ms" + if (r.tlsOk) " · TLS ✓" else "",
                                color = if (r.tlsOk) SpoofColors.ConnectedGreen else SpoofColors.TextSecondary,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }

        item {
            ToolCard(accent = accent) {
                SectionLabel(t("Fake SNI probe", "آزمون SNI جعلی"))
                Text(
                    t("Target edge", "مرجع هدف") + ": $targetEdge",
                    color = SpoofColors.TextSecondary, fontSize = 11.sp,
                )
                Spacer(Modifier.height(8.dp))
                ActionButton(
                    label = if (scanning == "sni") t("Probing…", "در حال آزمون…") else t("Scan fake SNIs", "اسکن SNIهای جعلی"),
                    accent = accent,
                    enabled = scanning == null,
                ) {
                    scanning = "sni"
                    scope.launch {
                        val results = Scanner.scanSnis(
                            snis = Scanner.FAKE_SNIS,
                            ip = targetEdge,
                            protector = null,
                        )
                        sniResults = results
                        results.firstOrNull { it.ok }?.let { best ->
                            vm.profileStore.selected()?.let { p ->
                                vm.saveProfile(p.copy(fakeSni = best.sni))
                            }
                            VpnStateStore.log("scanner: best fake SNI ${best.sni}")
                        }
                        scanning = null
                    }
                }
                if (sniResults.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    sniResults.forEach { r ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable {
                                    if (r.ok) {
                                        vm.profileStore.selected()?.let { p ->
                                            vm.saveProfile(p.copy(fakeSni = r.sni))
                                        }
                                    }
                                },
                        ) {
                            Text(
                                if (r.ok) "✓" else "✗",
                                color = if (r.ok) SpoofColors.ConnectedGreen else SpoofColors.ErrorRed,
                                fontSize = 13.sp,
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(r.sni, color = SpoofColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text(
                                if (r.ok) "${r.latencyMs} ms" else t("blocked", "مسدود"),
                                color = if (r.ok) SpoofColors.ConnectedGreen else SpoofColors.ErrorRed,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
