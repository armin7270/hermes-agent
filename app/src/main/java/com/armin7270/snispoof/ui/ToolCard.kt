package com.armin7270.snispoof.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armin7270.snispoof.ui.theme.SpoofColors

@Composable
internal fun ToolCard(
    modifier: Modifier = Modifier,
    accent: Color = SpoofColors.DisconnectedBlue,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ToolCardBrush, ToolCardShape)
            .border(1.dp, accent.copy(alpha = 0.24f), ToolCardShape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(15.dp),
    ) {
        content()
    }
}

@Composable
internal fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val font = homeLocalizedFont()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(accent.copy(alpha = 0.11f), RoundedCornerShape(11.dp))
                .border(1.dp, accent.copy(alpha = 0.24f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.size(12.dp))
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
        trailing?.invoke()
    }
}
