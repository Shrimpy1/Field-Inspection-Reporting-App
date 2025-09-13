package com.tiramisu.groupapplication.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant

@Entity(tableName = "inspection_logs")
data class InspectionLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val createdAt: Instant? = Clock.System.now(),
    val inspectionId: String,
    val info: String,
    val pendingSync: Boolean = false
)