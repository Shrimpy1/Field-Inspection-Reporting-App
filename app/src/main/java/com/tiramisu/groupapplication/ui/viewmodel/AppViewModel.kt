package com.tiramisu.groupapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.tiramisu.groupapplication.ui.component.BottomBarConfig
import com.tiramisu.groupapplication.ui.component.FabConfig
import com.tiramisu.groupapplication.ui.component.SnackbarConfig
import com.tiramisu.groupapplication.ui.component.TopBarConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor() : ViewModel() {

    private val _topBar = MutableStateFlow(TopBarConfig())
    val topBar: StateFlow<TopBarConfig> = _topBar

    private val _snackbar = MutableStateFlow(SnackbarConfig())
    val snackbar: StateFlow<SnackbarConfig> = _snackbar

    private val _bottomBar = MutableStateFlow(BottomBarConfig())
    val bottomBar: StateFlow<BottomBarConfig> = _bottomBar

    private val _fab = MutableStateFlow(FabConfig())
    val fab: StateFlow<FabConfig> = _fab

    fun setTopBar(state: TopBarConfig) {
        _topBar.value = state
    }

    fun clearTopBar() {
        _topBar.value = TopBarConfig()
    }

    fun setBottomBar(state: BottomBarConfig) {
        _bottomBar.value = state
    }

    fun clearBottomBar() {
        _bottomBar.value = BottomBarConfig()
    }

    fun setFab(state: FabConfig) {
        _fab.value = state
    }

    fun clearFab() {
        _fab.value = FabConfig()
    }

    fun setSnackbar(state: SnackbarConfig) {
        _snackbar.value = state
    }

    fun clearSnackbar() {
        _snackbar.value = SnackbarConfig()
    }

    fun reset() {
        clearTopBar()
        clearBottomBar()
        clearFab()
        clearSnackbar()
    }
}