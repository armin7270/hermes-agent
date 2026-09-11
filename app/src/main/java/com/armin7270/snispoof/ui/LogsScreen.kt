package com.armin7270.snispoof.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armin7270.snispoof.state.VpnViewModel
import com.armin7270.snispoof.ui.theme.SpoofColors

@Composable
internal fun LogsScreen(vm: VpnViewModel, onMenuClick: () -> Unit) {
    val accent = SpoofColors.ConnectingCyan
    val logs by vm.logs.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }
    ToolPageScaffold(
        accent = accent,
        header = {
            ToolPageHeader(
                title = t("Live logs", "لاگ زنده"),
                subtitle = t("Engine diagnostics and relay events", "گزارش‌های فنی موتور و اتصال‌ها"),
                icon = Icons.Rounded.BugReport,
                accent = accent,
                onMenuClick = onMenuClick,
            )
        },
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(640.dp)
                    .background(Color(0xB307111D), RoundedCornerShape(16.dp))
                    .border(1.dp, SpoofColors.CardBorder, RoundedCornerShape(16.dp)),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                ) {
                    items(logs) { line ->
                        Text(
                            line,
                            color = SpoofColors.TextSecondary,
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
        item {
            Text(
                t("Tap CONNECT on the home screen to start the tunnel and watch live events here.",
                  "دکمه اتصال را در خانه بزنید تا رویدادهای زنده اینجا نمایش داده شود."),
                color = SpoofColors.TextSecondary.copy(alpha = 0.8f),
                fontSize = 11.sp,
            )
        }
    }
}
