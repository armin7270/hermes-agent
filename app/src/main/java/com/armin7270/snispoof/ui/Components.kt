package com.armin7270.snispoof.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armin7270.snispoof.state.ConnectionState
import com.armin7270.snispoof.ui.theme.SpoofColors
import com.armin7270.snispoof.ui.theme.colorsFor

@Composable
fun ToolPageBackground(accent: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        SpoofColors.BackgroundTop,
                        SpoofColors.BackgroundMiddle,
                        SpoofColors.BackgroundBottom,
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.075f), Color.Transparent),
                        radius = 760f,
                    )
                )
        )
        content()
    }
}

val ToolCardShape = RoundedCornerShape(20.dp)
val ToolCardBrush = Brush.linearGradient(listOf(Color(0xE6142231), Color(0xD90B1724)))

@Composable
fun ToolCard(
    modifier: Modifier = Modifier,
    accent: Color = SpoofColors.DisconnectedBlue,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ToolCardBrush, ToolCardShape)
            .border(1.dp, accent.copy(alpha = 0.24f), ToolCardShape)
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        content()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = if (L10n.isPersian) text else text.uppercase(),
        color = SpoofColors.TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = if (L10n.isPersian) 0.sp else 1.2.sp,
        textAlign = if (L10n.isPersian) TextAlign.End else TextAlign.Start,
        modifier = modifier.fillMaxWidth().padding(horizontal = 3.dp),
    )
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = SpoofColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = SpoofColors.TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
        Spacer(Modifier.size(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = accent,
                checkedThumbColor = SpoofColors.BackgroundTop,
            ),
        )
    }
}

@Composable
fun NumberFieldRow(
    label: String,
    value: Int,
    accent: Color,
    suffix: String = "",
    onCommit: (Int) -> Unit,
) {
    var text by androidx.compose.runtime.remember(value) {
        androidx.compose.runtime.mutableStateOf(value.toString())
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = SpoofColors.TextPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.size(10.dp))
        TextField(
            value = text,
            onValueChange = { v -> text = v.filter { it.isDigit() }.take(5) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = SpoofColors.TextPrimary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SpoofColors.ButtonCenter,
                unfocusedContainerColor = SpoofColors.ButtonEdge,
                focusedIndicatorColor = accent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = accent,
            ),
            modifier = Modifier.width(96.dp).height(52.dp),
        )
        if (suffix.isNotBlank()) {
            Spacer(Modifier.size(6.dp))
            Text(suffix, color = SpoofColors.TextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.size(6.dp))
        androidx.compose.material3.TextButton(onClick = {
            val n = text.toIntOrNull() ?: return@TextButton
            onCommit(n)
        }) {
            Text(t("Save", "ذخیره"), color = accent, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun TextFieldRow(
    label: String,
    value: String,
    accent: Color,
    hint: String = "",
    onCommit: (String) -> Unit,
) {
    var text by androidx.compose.runtime.remember(value) {
        androidx.compose.runtime.mutableStateOf(value)
    }
    Column {
        Text(label, color = SpoofColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.size(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = text,
                onValueChange = { text = it.take(255) },
                singleLine = true,
                placeholder = { Text(hint, color = SpoofColors.TextSecondary, fontSize = 13.sp) },
                textStyle = androidx.compose.ui.text.TextStyle(color = SpoofColors.TextPrimary, fontSize = 14.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SpoofColors.ButtonCenter,
                    unfocusedContainerColor = SpoofColors.ButtonEdge,
                    focusedIndicatorColor = accent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = accent,
                ),
                modifier = Modifier.weight(1f).height(54.dp),
            )
            Spacer(Modifier.size(6.dp))
            androidx.compose.material3.TextButton(onClick = { onCommit(text.trim()) }) {
                Text(t("Save", "ذخیره"), color = accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * The big animated connect button — a faithful but simplified port of the
 * reference app's ConnectButton (breathing halo rings, loading arcs while
 * connecting, power glyph).
 */
@Composable
fun ConnectButton(
    state: ConnectionState,
    accent: Color,
    diameter: Dp = 210.dp,
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "connect-glow")
    val glow by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(tween(920, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1350, easing = LinearEasing)),
        label = "rotation",
    )
    val sweep by transition.animateFloat(
        initialValue = 58f,
        targetValue = 292f,
        animationSpec = infiniteRepeatable(tween(880, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sweep",
    )
    val halo = 54.dp
    val glowIntensity = if (state == ConnectionState.CONNECTING) glow else 1f

    Box(
        modifier = Modifier
            .size(diameter + halo)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val surfaceRadius = diameter.toPx() / 2f
            val atmosphericRadius = size.minDimension / 2f
            val haloPx = halo.toPx()

            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        0.54f to accent.copy(alpha = 0.015f * glowIntensity),
                        0.67f to accent.copy(alpha = 0.20f * glowIntensity),
                        0.76f to accent.copy(alpha = 0.34f * glowIntensity),
                        0.87f to accent.copy(alpha = 0.14f * glowIntensity),
                        1f to Color.Transparent,
                    ),
                    center = center,
                    radius = atmosphericRadius,
                ),
                center = center,
                radius = atmosphericRadius,
            )
            drawCircle(
                color = accent.copy(alpha = 0.075f * glowIntensity),
                radius = surfaceRadius + haloPx * 0.50f,
                center = center,
                style = Stroke(1.dp.toPx()),
            )
            drawCircle(
                color = accent.copy(alpha = 0.29f * glowIntensity),
                radius = surfaceRadius + haloPx * 0.15f,
                center = center,
                style = Stroke(7.dp.toPx()),
            )
            if (state == ConnectionState.CONNECTING) {
                val r = surfaceRadius + haloPx * 0.15f
                drawArc(
                    color = accent.copy(alpha = 0.18f),
                    startAngle = rotation,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(8.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            // surface
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to SpoofColors.ButtonCenter,
                        1f to SpoofColors.ButtonEdge,
                    ),
                    center = center,
                    radius = surfaceRadius,
                ),
                radius = surfaceRadius,
                center = center,
            )
            drawCircle(
                color = SpoofColors.ButtonInnerRing.copy(alpha = 0.55f),
                radius = surfaceRadius - 6.dp.toPx(),
                center = center,
                style = Stroke(1.4.dp.toPx()),
            )
        }
        Icon(
            imageVector = Icons.Rounded.PowerSettingsNew,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(diameter * 0.34f),
        )
    }
}

@Composable
fun ActionButton(label: String, accent: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(
                if (enabled) accent.copy(alpha = 0.18f) else Color(0x33FFFFFF),
                RoundedCornerShape(14.dp),
            )
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun StatChip(label: String, value: String, accent: Color) {
    Column(
        modifier = Modifier
            .background(Color(0x14000000), RoundedCornerShape(12.dp))
            .border(1.dp, SpoofColors.CardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = SpoofColors.TextSecondary, fontSize = 10.sp)
    }
}

fun formatBytes(n: Long): String = when {
    n >= 1 shl 30 -> "%.2f GB".format(n / 1073741824.0)
    n >= 1 shl 20 -> "%.1f MB".format(n / 1048576.0)
    n >= 1 shl 10 -> "%.1f KB".format(n / 1024.0)
    else -> "$n B"
}

fun formatUptime(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
fun rememberAccent(state: ConnectionState): Color = remember(state) { colorsFor(state).accent }
