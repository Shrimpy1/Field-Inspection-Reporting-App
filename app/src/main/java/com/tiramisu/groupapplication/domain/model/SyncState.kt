package com.tiramisu.groupapplication.domain.model

data class SyncState(
    val syncing: Boolean = false,
    val error: Throwable? = null
)
