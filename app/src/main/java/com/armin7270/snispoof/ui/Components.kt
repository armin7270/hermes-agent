package com.armin7270.snispoof.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armin7270.snispoof.ui.theme.SpoofColors

@Composable
internal fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    onChange: (Boolean) -> Unit,
) {
    val font = homeLocalizedFont()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = SpoofColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = font,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    color = SpoofColors.TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    fontFamily = font,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
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
internal fun NumberFieldRow(
    label: String,
    value: Int,
    accent: Color,
    suffix: String = "",
    onCommit: (Int) -> Unit,
) {
    val font = homeLocalizedFont()
    var text by remember(value) { mutableStateOf(value.toString()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            color = SpoofColors.TextPrimary,
            fontSize = 14.5.sp,
            fontFamily = font,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        TextField(
            value = text,
            onValueChange = { v -> text = v.filter { it.isDigit() }.take(5) },
            singleLine = true,
            textStyle = TextStyle(
                color = SpoofColors.TextPrimary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                fontFamily = font,
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
            Spacer(Modifier.width(6.dp))
            Text(suffix, color = SpoofColors.TextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.width(6.dp))
        androidx.compose.material3.TextButton(onClick = {
            val n = text.toIntOrNull() ?: return@TextButton
            onCommit(n)
        }) {
            Text(t("Save", "ذخیره"), color = accent, fontWeight = FontWeight.SemiBold, fontFamily = font)
        }
    }
}

@Composable
internal fun TextFieldRow(
    label: String,
    value: String,
    accent: Color,
    hint: String = "",
    onCommit: (String) -> Unit,
) {
    val font = homeLocalizedFont()
    var text by remember(value) { mutableStateOf(value) }
    Column {
        Text(
            label,
            color = SpoofColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = font,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = text,
                onValueChange = { text = it.take(4096) },
                singleLine = true,
                placeholder = {
                    Text(hint, color = SpoofColors.TextSecondary, fontSize = 12.5.sp, maxLines = 1)
                },
                textStyle = TextStyle(color = SpoofColors.TextPrimary, fontSize = 13.5.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SpoofColors.ButtonCenter,
                    unfocusedContainerColor = SpoofColors.ButtonEdge,
                    focusedIndicatorColor = accent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = accent,
                ),
                modifier = Modifier.weight(1f).height(54.dp),
            )
            Spacer(Modifier.width(6.dp))
            androidx.compose.material3.TextButton(onClick = { onCommit(text.trim()) }) {
                Text(t("Save", "ذخیره"), color = accent, fontWeight = FontWeight.SemiBold, fontFamily = font)
            }
        }
    }
}

@Composable
internal fun ActionButton(label: String, accent: Color, enabled: Boolean = true, onClick: () -> Unit) {
    val font = homeLocalizedFont()
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
        Text(label, color = accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = font)
    }
}
