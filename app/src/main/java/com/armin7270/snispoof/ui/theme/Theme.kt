package com.armin7270.snispoof.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.armin7270.snispoof.state.ConnectionState

/**
 * Palette lifted from the reference app's UacColors: deep navy background,
 * state-driven accent (blue → cyan → green → amber → red).
 */
object SpoofColors {
    val BackgroundTop = Color(0xFF020913)
    val BackgroundMiddle = Color(0xFF04101C)
    val BackgroundBottom = Color(0xFF071421)
    val Surface = Color(0xFF101C29)
    val SurfaceElevated = Color(0xFF16243A)
    val DisconnectedBlue = Color(0xFF299EFF)
    val ConnectingCyan = Color(0xFF27D7FF)
    val ConnectedGreen = Color(0xFF25F58A)
    val DisconnectingAmber = Color(0xFFFFB44A)
    val ErrorRed = Color(0xFFFF3344)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8D99A6)
    val CardBorder = Color(0x20FFFFFF)
    val Divider = Color(0x24FFFFFF)
    val ButtonCenter = Color(0xFF172536)
    val ButtonEdge = Color(0xFF07111D)
    val ButtonInnerRing = Color(0xFF40536A)
}

data class UiStateColors(val accent: Color)

fun colorsFor(state: ConnectionState): UiStateColors = when (state) {
    ConnectionState.DISCONNECTED -> UiStateColors(SpoofColors.DisconnectedBlue)
    ConnectionState.CONNECTING -> UiStateColors(SpoofColors.ConnectingCyan)
    ConnectionState.CONNECTED -> UiStateColors(SpoofColors.ConnectedGreen)
    ConnectionState.DISCONNECTING -> UiStateColors(SpoofColors.DisconnectingAmber)
    ConnectionState.ERROR -> UiStateColors(SpoofColors.ErrorRed)
}

private val DarkScheme = darkColorScheme(
    primary = SpoofColors.ConnectedGreen,
    secondary = SpoofColors.DisconnectedBlue,
    tertiary = SpoofColors.ConnectingCyan,
    background = SpoofColors.BackgroundTop,
    surface = SpoofColors.Surface,
    surfaceVariant = SpoofColors.SurfaceElevated,
    onPrimary = SpoofColors.BackgroundTop,
    onBackground = SpoofColors.TextPrimary,
    onSurface = SpoofColors.TextPrimary,
    error = SpoofColors.ErrorRed,
)

@Composable
fun SpoofTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        content = content,
    )
}
