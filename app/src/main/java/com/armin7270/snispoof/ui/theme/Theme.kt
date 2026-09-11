package com.armin7270.snispoof.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import com.armin7270.snispoof.R
import com.armin7270.snispoof.state.ConnectionState

object SpoofColors {
    val BackgroundTop = Color(0xFF020913)
    val BackgroundMiddle = Color(0xFF04101C)
    val BackgroundBottom = Color(0xFF071421)
    val Surface = Color(0xFF101C29)
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

val VazirmatnUiFd = FontFamily(
    Font(R.font.vazirmatn_ui_fd_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_ui_fd_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_ui_fd_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_ui_fd_bold, FontWeight.Bold),
)

private val DarkScheme = darkColorScheme(
    primary = SpoofColors.ConnectedGreen,
    secondary = SpoofColors.DisconnectedBlue,
    tertiary = SpoofColors.ConnectingCyan,
    background = SpoofColors.BackgroundTop,
    surface = SpoofColors.Surface,
    onPrimary = SpoofColors.BackgroundTop,
    onBackground = SpoofColors.TextPrimary,
    onSurface = SpoofColors.TextPrimary,
    error = SpoofColors.ErrorRed,
)

/** Locks the system font scale so the UAC-style layout never reflows. */
@Composable
fun ProvideFixedFontScale(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val fixed = androidx.compose.runtime.remember(density.density) {
        Density(density.density, fontScale = 1f)
    }
    CompositionLocalProvider(LocalDensity provides fixed, content = content)
}

@Composable
fun SpoofTheme(content: @Composable () -> Unit) {
    ProvideFixedFontScale {
        MaterialTheme(colorScheme = DarkScheme, content = content)
    }
}
