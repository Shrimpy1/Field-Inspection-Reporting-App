package com.tiramisu.groupapplication.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.tiramisu.groupapplication.domain.enums.InspectionStatus
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Entity(tableName = "inspection_reports")
data class Inspection @OptIn(ExperimentalTime::class) constructor(
    @PrimaryKey @get:Exclude @set:Exclude var id: String = "",
    val createdAt: Instant = Clock.System.now(),
    val type: String,
    val title: String,
    val notes: String,
    val score: String,
    val outcome: String,
    val status: InspectionStatus,
    val lat: Double?,
    val lng: Double?,
    val isSynced: Boolean = false
)