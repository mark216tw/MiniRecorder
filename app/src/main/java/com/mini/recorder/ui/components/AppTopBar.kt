package com.mini.recorder.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val EXTRA_TOP_SPACING = 4.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.getTop(density)
    val extraPx = with(density) { EXTRA_TOP_SPACING.toPx() }
    val insets = WindowInsets(
        left = 0.dp,
        top = with(density) { (statusBarTop + extraPx).toDp() },
        right = 0.dp,
        bottom = 0.dp
    )
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        windowInsets = insets
    )
}
