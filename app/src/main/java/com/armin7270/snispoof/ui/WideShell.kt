package com.armin7270.snispoof.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val LocalWideShell = compositionLocalOf { false }

internal object WideShell {
    val EdgePadding: Dp = 28.dp
    val HomeContentMax: Dp = 560.dp
    val PageContentMax: Dp = 560.dp

    fun isWide(width: Dp, height: Dp): Boolean = width >= 560.dp && height >= 640.dp
}

@Composable
internal fun ProvideWideShell(isWide: Boolean, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalWideShell provides isWide, content = content)
}
