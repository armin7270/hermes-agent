package com.armin7270.snispoof.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Bilingual UI strings (Persian-first) — same approach as the reference app's
 * homeText(en, fa) helper. Language is driven by the settings screen.
 */
object L10n {
    val language: MutableState<String> = mutableStateOf("fa")

    val isPersian: Boolean get() = language.value == "fa"
}

@Composable
fun t(en: String, fa: String): String = if (L10n.isPersian) fa else en

/** Non-composable variant for callbacks. */
fun tNoCompose(en: String, fa: String): String = if (L10n.isPersian) fa else en
