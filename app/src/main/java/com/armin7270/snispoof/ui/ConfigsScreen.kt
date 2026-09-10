package com.armin7270.snispoof.ui

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armin7270.snispoof.core.proxy.ProxyProtocol
import com.armin7270.snispoof.state.VpnViewModel
import com.armin7270.snispoof.ui.theme.SpoofColors

/** Imported proxy configs (vless/trojan/vmess) — the UAC "Configs" screen. */
@Composable
fun ConfigsScreen(vm: VpnViewModel, onBack: () -> Unit) {
    val configs by vm.configs.collectAsStateWithLifecycle()
    val selectedId by vm.selectedConfigId.collectAsStateWithLifecycle()
    val accent = SpoofColors.ConnectingCyan
    val clipboard = LocalClipboardManager.current
    var notice by remember { mutableStateOf<String?>(null) }
    val lblPaste = t("Paste config link(s) below", "لینک کانفیگ را اینجا بچسبانید")
    val lblImport = t("Import from clipboard", "ورود از کلیپ‌بورد")
    val hint = "vless://…  trojan://…  vmess://…"
    val okEmpty = t("No configs yet — the tunnel runs direct with DPI desync.",
        "هنوز کانفیگی نیست — تونل مستقیم با desync اجرا می‌شود.")
    val lblDirect = t("Use direct mode (no tunnel)", "حالت مستقیم (بدون تونل)")
    val msgDirect = t("Direct mode (no config)", "حالت مستقیم (بدون کانفیگ)")

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
                    Text(t("Configs", "کانفیگ‌ها"), color = SpoofColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        t("VLESS / Trojan / VMess — tunnel all traffic", "VLESS / Trojan / VMess — تونل همه ترافیک"),
                        color = SpoofColors.TextSecondary, fontSize = 11.sp,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    ToolCard(accent = accent) {
                        Text(
                            lblPaste,
                            color = SpoofColors.TextSecondary, fontSize = 12.sp,
                        )
                        TextFieldRow(
                            label = lblImport,
                            value = "",
                            accent = accent,
                            onCommit = { text ->
                                val payload = if (text.isBlank())
                                    clipboard.getText()?.text.orEmpty() else text
                                val n = vm.importConfigs(payload)
                                notice = if (n > 0) tNoCompose("Imported $n config(s)", "$n کانفیگ وارد شد")
                                else tNoCompose("No valid config found", "کانفیگ معتبری پیدا نشد")
                            },
                            hint = hint,
                        )
                        if (notice != null) {
                            Text(notice!!, color = accent, fontSize = 12.sp)
                        }
                    }
                }

                if (configs.isEmpty()) {
                    item {
                        ToolCard(accent = accent) {
                            Text(okEmpty, color = SpoofColors.TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                items(configs.size) { i ->
                    val c = configs[i]
                    val selected = c.id == selectedId
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.selectConfig(c.id) }
                            .background(
                                if (selected) accent.copy(alpha = 0.10f) else Color.Transparent,
                                RoundedCornerShape(12.dp),
                            )
                            .border(
                                1.dp,
                                if (selected) accent else SpoofColors.CardBorder,
                                RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle, null,
                            tint = if (selected) accent else Color.Transparent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, color = SpoofColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(
                                "${c.protocol.uppercase()} · ${c.address}:${c.port} · ${c.net.id}${if (c.tls) " +tls" else ""} · ${c.sni.ifBlank { "-" }}",
                                color = SpoofColors.TextSecondary, fontSize = 11.sp, maxLines = 1,
                            )
                        }
                        Text(
                            when (c.protocol) {
                                ProxyProtocol.VLESS.id -> "VLESS"
                                ProxyProtocol.TROJAN.id -> "TROJAN"
                                else -> "VMESS"
                            },
                            color = SpoofColors.TextSecondary, fontSize = 10.sp,
                        )
                        IconButton(onClick = { vm.deleteConfig(c.id) }) {
                            Icon(Icons.Rounded.Delete, null, tint = SpoofColors.ErrorRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                item {
                    TextButton(onClick = {
                        vm.selectConfig(null)
                        notice = msgDirect
                    }) {
                        Text(lblDirect, color = SpoofColors.TextSecondary)
                    }
                }
            }
        }
    }
}
