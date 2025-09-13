package com.tiramisu.groupapplication.domain.repository

interface LogRepository {
    suspend fun log(
        inspectionId: String,
        info: String = "",
    ): Any

    suspend fun filter(days: Int = 7)
    suspend fun pushPendingChanges()
}