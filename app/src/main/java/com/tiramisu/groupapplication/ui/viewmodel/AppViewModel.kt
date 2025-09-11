package com.tiramisu.groupapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiramisu.groupapplication.domain.repository.InspectionRepository
import com.tiramisu.groupapplication.util.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    connectivityObserver: ConnectivityObserver,
    private val repo: InspectionRepository
) : ViewModel() {
    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    // Back online: reconcile
                    repo.pushPendingChanges()
                    repo.pullRemoteChanges()
                }
        }
    }
}