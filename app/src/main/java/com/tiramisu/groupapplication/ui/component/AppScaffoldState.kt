package com.tiramisu.groupapplication.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable

data class TopBarConfig(
    val visible: Boolean = false,
    val title: @Composable () -> Unit = {},
    val navIcon: @Composable () -> Unit = {},
    val actions: @Composable RowScope.() -> Unit = {}
)

data class SnackbarConfig(
    val snackbarHost: @Composable () -> Unit = {},
)

data class BottomBarConfig(
    val visible: Boolean = false,
    val content: @Composable () -> Unit = {}
)

data class FabConfig(
    val visible: Boolean = false,
    val content: @Composable () -> Unit = {}
)