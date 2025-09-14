package com.tiramisu.groupapplication.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.tiramisu.groupapplication.domain.enums.InspectionStatus
import com.tiramisu.groupapplication.domain.enums.InspectionType
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Entity(tableName = "inspection_reports")
data class Inspection @OptIn(ExperimentalTime::class) constructor(
    @PrimaryKey @get:Exclude @set:Exclude var id: String = UUID.randomUUID().toString()
        .replace("-", ""),
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now(),
    val type: InspectionType,
    val title: String,
    val notes: String,
    val score: String,
    val outcome: String,
    val status: InspectionStatus,
    val lat: Double?,
    val lng: Double?,
    val pendingSync: Boolean = false,
    val reviewNotes: String? = null,
    val reviewedBy: String? = null,
    val reviewedAt: Instant? = null,
    val createdBy: String? = null,
    val deletedAt: String? = null,
)