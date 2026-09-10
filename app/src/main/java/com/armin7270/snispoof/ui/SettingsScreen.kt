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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armin7270.snispoof.core.desync.DesyncMethod
import com.armin7270.snispoof.core.engine.SpoofProfile
import com.armin7270.snispoof.state.DnsProvider
import com.armin7270.snispoof.state.PerAppMode
import com.armin7270.snispoof.state.VpnViewModel
import com.armin7270.snispoof.ui.theme.SpoofColors

@Composable
fun SettingsScreen(vm: VpnViewModel, onBack: () -> Unit, onOpenApps: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val selectedId by vm.selectedProfileId.collectAsStateWithLifecycle()
    val accent = SpoofColors.DisconnectedBlue
    var editingProfile by remember { mutableStateOf<SpoofProfile?>(null) }
    var importText by remember { mutableStateOf<String?>(null) }

    ToolPageBackground(accent) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = SpoofColors.TextPrimary)
                }
                Icon(Icons.Rounded.Settings, null, tint = accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(t("Settings", "تنظیمات"), color = SpoofColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(t("Engine, DNS and routing", "هسته، DNS و مسیریابی"), color = SpoofColors.TextSecondary, fontSize = 11.sp)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 18.dp, end = 18.dp, bottom = 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {

                // ---- DPI evasion mode ----
                item {
                    ToolCard(accent = accent) {
                        SectionLabel(t("DPI evasion mode", "حالت دورزدن DPI"))
                        Spacer(Modifier.height(4.dp))
                        ChipRow(
                            options = listOf(
                                DesyncMethod.SPLIT_N.id to t("Fragment", "قطعه‌بندی"),
                                DesyncMethod.SNI_REPLACE.id to t("Fake SNI", "SNI جعلی"),
                                DesyncMethod.COMBINED.id to t("Combined", "ترکیبی"),
                            ),
                            selected = selectedDesync(profiles, selectedId),
                            accent = accent,
                        ) { methodId ->
                            val current = vm.profileStore.selected()
                            if (current != null) {
                                vm.saveProfile(current.copy(desyncMethod = methodId))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            t(
                                "Fragment splits the TLS ClientHello into separate TCP segments. Fake SNI replaces the real hostname. Combined does both.",
                                "قطعه‌بندی ClientHello را به چند سگمنت TCP می‌شکند. SNI جعلی نام دامنه واقعی را عوض می‌کند. ترکیبی هر دو را انجام می‌دهد."
                            ),
                            color = SpoofColors.TextSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }

                // ---- fragment / split tuning ----
                item {
                    ToolCard(accent = accent) {
                        SectionLabel(t("Fragmentation", "قطعه‌بندی"))
                        val p = vm.profileStore.selected()
                        NumberFieldRow(
                            label = t("Split position (bytes)", "موقعیت برش (بایت)"),
                            value = p?.splitN ?: 2,
                            accent = accent,
                            onCommit = { n -> p?.let { vm.saveProfile(it.copy(splitN = n.coerceIn(1, 255))) } },
                        )
                        NumberFieldRow(
                            label = t("Delay after first chunk", "تاخیر بعد از قطعه اول"),
                            value = p?.delayMs ?: 25,
                            accent = accent,
                            onCommit = { n -> p?.let { vm.saveProfile(it.copy(delayMs = n.coerceIn(0, 500))) } },
                            suffix = "ms",
                        )
                        NumberFieldRow(
                            label = t("Fragments (multi-frag mode)", "تعداد قطعات (حالت چندقطعه)"),
                            value = p?.fragmentCount ?: 4,
                            accent = accent,
                            onCommit = { n -> p?.let { vm.saveProfile(it.copy(fragmentCount = n.coerceIn(2, 64))) } },
                        )                    }
                }

                // ---- fake host ----
                item {
                    ToolCard(accent = accent) {
                        SectionLabel(t("Fake SNI host", "دامنه SNI جعلی"))
                        TextFieldRow(
                            label = t("Custom fake host", "دامنه جعلی سفارشی"),
                            value = vm.profileStore.selected()?.fakeSni ?: "auth.vercel.com",
                            accent = accent,
                            onCommit = { host ->
                                vm.profileStore.selected()?.let {
                                    vm.saveProfile(it.copy(fakeSni = host.ifBlank { "auth.vercel.com" }))
                                }
                            },
                            hint = "auth.vercel.com",
                        )
                        ChipRow(
                            options = listOf(
                                "auth.vercel.com" to "Vercel",
                                "www.speedtest.net" to "Speedtest",
                                "mci.ir" to "MCI",
                                "www.google.com" to "Google",
                            ),
                            selected = null,
                            accent = accent,
                        ) { host ->
                            vm.profileStore.selected()?.let {
                                vm.saveProfile(it.copy(fakeSni = host))
                            }
                        }
                    }
                }

                // ---- profiles ----
                item {
                    val newProfileName = t("New profile", "پروفایل جدید")
                    ToolCard(accent = accent) {
                        SectionLabel(t("Profiles (patterniha config.json)", "پروفایل‌ها (config.json پترنی‌ها)"))
                        profiles.forEach { p ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.selectProfile(p.id) }
                                    .background(
                                        if (p.id == selectedId) accent.copy(alpha = 0.10f) else Color.Transparent,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(12.dp)
                                        .background(
                                            if (p.id == selectedId) accent else Color.Transparent,
                                            RoundedCornerShape(50)
                                        )
                                        .border(
                                            1.dp,
                                            if (p.id == selectedId) accent else SpoofColors.TextSecondary,
                                            RoundedCornerShape(50)
                                        )
                                )
                                Spacer(Modifier.size(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, color = SpoofColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${p.connectIp}:${p.connectPort} · ${p.fakeSni} · ${p.desyncMethod}",
                                        color = SpoofColors.TextSecondary, fontSize = 11.sp,
                                    )
                                }
                                IconButton(onClick = { editingProfile = p }) {
                                    Icon(Icons.Rounded.Settings, null, tint = SpoofColors.TextSecondary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { vm.deleteProfile(p.id) }) {
                                    Icon(Icons.Rounded.Delete, null, tint = SpoofColors.ErrorRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        TextButton(onClick = {
                            vm.saveProfile(
                                SpoofProfile(
                                    id = "p-" + System.currentTimeMillis(),
                                    name = newProfileName,
                                )
                            )
                        }) {
                            Icon(Icons.Rounded.Add, null, tint = accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(t("Add profile", "افزودن پروفایل"), color = accent)
                        }
                    }
                }

                // ---- DNS ----
                item {
                    ToolCard(accent = accent) {
                        SectionLabel("DNS")
                        ChipRow(
                            options = DnsProvider.entries.map { it.id to it.label },
                            selected = settings.dnsProvider.id,
                            accent = accent,
                        ) { id -> vm.setDnsProvider(DnsProvider.fromId(id)) }
                        if (settings.dnsProvider == DnsProvider.CUSTOM) {
                            TextFieldRow(
                                label = t("Custom DNS IP", "آی‌پی DNS سفارشی"),
                                value = settings.customDns,
                                accent = accent,
                                onCommit = { vm.setCustomDns(it) },
                                hint = "1.1.1.1",
                            )
                        }
                    }
                }

                // ---- network ----
                item {
                    ToolCard(accent = accent) {
                        SectionLabel(t("Network", "شبکه"))
                        SwitchRow(
                            title = t("Block QUIC (UDP 443)", "مسدودسازی QUIC (UDP 443)"),
                            subtitle = t("Apps fall back to TCP where desync applies", "برنامه‌ها به TCP برمی‌گردند تا قطعه‌بندی اعمال شود"),
                            checked = settings.blockQuic,
                            accent = accent,
                            onChange = { vm.setBlockQuic(it) },
                        )
                        NumberFieldRow(
                            label = "MTU",
                            value = settings.mtu,
                            accent = accent,
                            onCommit = { vm.setMtu(it) },
                        )
                        SwitchRow(
                            title = t("Root mode (wrong_seq injection)", "حالت روت (تزریق wrong_seq)"),
                            subtitle = t("Faithful patterniha injection via su helper", "تزریق دقیق پترنی‌ها از طریق هلپر su"),
                            checked = settings.rootMode,
                            accent = accent,
                            onChange = { vm.setRootMode(it) },
                        )
                        SwitchRow(
                            title = t("Auto connect on boot", "اتصال خودکار بعد از روشن‌شدن"),
                            subtitle = "",
                            checked = settings.autoStartBoot,
                            accent = accent,
                            onChange = { vm.setAutoStartBoot(it) },
                        )
                    }
                }

                // ---- per-app ----
                item {
                    ToolCard(accent = accent) {
                        SectionLabel(t("Per-app proxy", "پروکسی هر برنامه"))
                        ChipRow(
                            options = listOf(
                                PerAppMode.ALL.id to t("All apps", "همه"),
                                PerAppMode.WHITELIST.id to t("Whitelist", "لیست سفید"),
                                PerAppMode.BLACKLIST.id to t("Blacklist", "لیست سیاه"),
                            ),
                            selected = settings.perAppMode.id,
                            accent = accent,
                        ) { vm.setPerAppMode(PerAppMode.fromId(it)) }
                        TextButton(onClick = onOpenApps) {
                            Text(
                                t("Choose apps (${settings.perAppPackages.size})", "انتخاب برنامه‌ها (${settings.perAppPackages.size})"),
                                color = accent,
                            )
                        }
                    }
                }

                // ---- import ----
                item {
                    ToolCard(accent = accent) {
                        SectionLabel(t("Import", "ورود کانفیگ"))
                        Text(
                            t("Paste a patterniha config.json or a shared profile JSON", "یک config.json پترنی‌ها یا JSON پروفایل بچسبانید"),
                            color = SpoofColors.TextSecondary, fontSize = 12.sp,
                        )
                        TextFieldRow(
                            label = t("Import JSON", "ورود JSON"),
                            value = "",
                            accent = accent,
                            onCommit = { text -> if (text.isNotBlank()) vm.importProfiles(text) },
                            hint = "{ \"CONNECT_IP\": ... }",
                        )
                    }
                }
            }
        }
    }

    editingProfile?.let { profile ->
        ProfileEditorSheet(profile, accent, vm) { editingProfile = null }
    }
}

private fun selectedDesync(profiles: List<SpoofProfile>, selectedId: String?): String =
    profiles.firstOrNull { it.id == selectedId }?.desyncMethod ?: DesyncMethod.SPLIT_N.id

@Composable
private fun ChipRow(
    options: List<Pair<String, String>>,
    selected: String?,
    accent: Color,
    onSelect: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (id, label) ->
            FilterChip(
                selected = selected == id,
                onClick = { onSelect(id) },
                label = { Text(label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent.copy(alpha = 0.18f),
                    selectedLabelColor = accent,
                    labelColor = SpoofColors.TextSecondary,
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditorSheet(
    profile: SpoofProfile,
    accent: Color,
    vm: VpnViewModel,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SpoofColors.Surface) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            SectionLabel(t("Edit profile", "ویرایش پروفایل"))
            Spacer(Modifier.height(10.dp))
            TextFieldRow(t("Name", "نام"), profile.name, accent) { vm.saveProfile(profile.copy(name = it.ifBlank { profile.name })); onDismiss() }
            TextFieldRow(t("Connect IP (CONNECT_IP)", "آی‌پی مقصد (CONNECT_IP)"), profile.connectIp, accent) { vm.saveProfile(profile.copy(connectIp = it)); onDismiss() }
            NumberFieldRow(t("Connect port", "پورت مقصد"), profile.connectPort, accent, onCommit = { vm.saveProfile(profile.copy(connectPort = it.coerceIn(1, 65535))); onDismiss() })
            TextFieldRow(t("Fake SNI", "SNI جعلی"), profile.fakeSni, accent) { vm.saveProfile(profile.copy(fakeSni = it)); onDismiss() }
            Spacer(Modifier.height(6.dp))
            Text(
                t("Match scope", "دامنه اعمال"),
                color = SpoofColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            )
            com.armin7270.snispoof.core.engine.MatchScope.entries.forEach { scope ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.saveProfile(profile.copy(matchScope = scope.id)); onDismiss() }
                        .padding(vertical = 8.dp),
                ) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .background(
                                if (profile.matchScope == scope.id) accent else Color.Transparent,
                                RoundedCornerShape(50)
                            )
                            .border(1.dp, if (profile.matchScope == scope.id) accent else SpoofColors.TextSecondary, RoundedCornerShape(50))
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(scope.name, color = SpoofColors.TextPrimary, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
