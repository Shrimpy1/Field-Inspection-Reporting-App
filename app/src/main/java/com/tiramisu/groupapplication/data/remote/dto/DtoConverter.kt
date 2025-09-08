package com.tiramisu.groupapplication.data.remote.dto

import com.google.firebase.Timestamp
import com.tiramisu.groupapplication.domain.enums.InspectionStatus
import com.tiramisu.groupapplication.domain.model.Inspection
import java.util.Date
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private fun InspectionStatus.toRaw(): String = name.lowercase()
private fun String.toInspectionStatus(): InspectionStatus =
    runCatching { InspectionStatus.valueOf(this.uppercase()) }.getOrElse { InspectionStatus.PENDING_REVIEW }

@OptIn(ExperimentalTime::class)
fun InspectionDto.toInspection(id: String): Inspection = Inspection(
    createdAt = this.createdAt?.toDate()?.time?.let { Instant.fromEpochMilliseconds(it) }
        ?: Clock.System.now(),
    id = id,
    type = this.type,
    title = this.title,
    notes = this.notes,
    score = this.score,
    outcome = this.outcome,
    status = this.status.toInspectionStatus(),
    lat = this.latitude,
    lng = this.longitude,
    isSynced = true
)

@OptIn(ExperimentalTime::class)
fun Inspection.toInspectionDto(): InspectionDto = InspectionDto(
    createdAt = Timestamp(Date(this.createdAt.toEpochMilliseconds())),
    type = this.type,
    title = this.title,
    notes = this.notes,
    score = this.score,
    outcome = this.outcome,
    status = this.status.toRaw(),
    latitude = this.lat,
    longitude = this.lng
)