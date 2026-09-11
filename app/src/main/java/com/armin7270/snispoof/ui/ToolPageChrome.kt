package com.armin7270.snispoof.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armin7270.snispoof.ui.theme.SpoofColors

@Composable
internal fun ToolPageBackground(
    accent: Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        SpoofColors.BackgroundTop,
                        SpoofColors.BackgroundMiddle,
                        SpoofColors.BackgroundBottom,
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.075f), Color.Transparent),
                        radius = 760f,
                    ),
                ),
        )
        content()
    }
}

@Composable
internal fun ToolPageScaffold(
    accent: Color,
    header: @Composable () -> Unit,
    verticalSpacing: Dp = 12.dp,
    content: LazyListScope.() -> Unit,
) {
    val wide = LocalWideShell.current
    val localizedTextStyle = homeLocalizedTextStyle()
    ToolPageBackground(accent) {
        CompositionLocalProvider(LocalTextStyle provides localizedTextStyle) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.safeDrawing.asPaddingValues()),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (wide) WideShell.EdgePadding else 18.dp)
                        .padding(top = 10.dp, bottom = 12.dp),
                ) {
                    header()
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
                        content = content,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ToolPageHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localizedFont = homeLocalizedFont()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0x99101C29), CircleShape)
                .background(
                    Brush.linearGradient(listOf(Color(0x14101C29), Color(0x1407111D))),
                    CircleShape,
                )
                .clickableNoIndication(onClick = onMenuClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Menu,
                contentDescription = "Open navigation",
                tint = SpoofColors.TextPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(accent.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0x1A299EFF), Color(0x0A299EFF))),
                    RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(23.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SpoofColors.TextPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = localizedFont,
                textAlign = TextAlign.Start,
            )
            Text(
                text = subtitle,
                color = SpoofColors.TextSecondary,
                fontSize = 11.sp,
                fontFamily = localizedFont,
                textAlign = TextAlign.Start,
            )
        }
    }
}

private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
        indication = null,
        onClick = onClick,
    )

@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val persian = LocalHomePersian.current
    Text(
        text = if (persian) text else text.uppercase(),
        color = SpoofColors.TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = homeLocalizedFont(),
        letterSpacing = if (persian) 0.sp else 1.2.sp,
        textAlign = if (persian) TextAlign.End else TextAlign.Start,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp),
    )
}

internal val ToolCardShape = RoundedCornerShape(20.dp)
internal val ToolCardBrush = Brush.linearGradient(
    listOf(Color(0xE6142231), Color(0xD90B1724)),
)
