package com.tiramisu.groupapplication.data.remote.dto

import com.tiramisu.groupapplication.domain.enums.InspectionStatus
import com.tiramisu.groupapplication.domain.enums.InspectionType
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.domain.model.InspectionLog
import com.tiramisu.groupapplication.util.ext.toKtInstant
import com.tiramisu.groupapplication.util.ext.toTimestamp
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private fun InspectionStatus.toRaw(): String = name.lowercase()
private fun String.toInspectionStatus(): InspectionStatus =
    runCatching { InspectionStatus.valueOf(this.uppercase()) }.getOrElse { InspectionStatus.PENDING_REVIEW }

@OptIn(ExperimentalTime::class)
fun InspectionDto.toInspection(id: String): Inspection = Inspection(
    createdAt = this.createdAt?.toKtInstant() ?: Clock.System.now(),
    updatedAt = this.updatedAt?.toKtInstant() ?: Clock.System.now(),
    id = id,
    type = InspectionType.valueOf(this.type),
    title = this.title,
    notes = this.notes,
    score = this.score,
    outcome = this.outcome,
    status = this.status.toInspectionStatus(),
    lat = this.latitude,
    lng = this.longitude,
    pendingSync = true,
    createdBy = this.createdBy,
)

@OptIn(ExperimentalTime::class)
fun Inspection.toInspectionDto(): InspectionDto = InspectionDto(
    createdAt = this.createdAt.toTimestamp(),
    updatedAt = this.updatedAt.toTimestamp(),
    type = this.type.name,
    title = this.title,
    notes = this.notes,
    score = this.score,
    outcome = this.outcome,
    status = this.status.toRaw(),
    latitude = this.lat,
    longitude = this.lng,
    createdBy = this.createdBy,
)

fun InspectionLog.toDto(): InspectionLogDto =
    InspectionLogDto(
        id = id,
        inspectionId = inspectionId,
        info = info,
        createdAt = this.createdAt?.toTimestamp(),
    )

fun InspectionLogDto.toDomain(): InspectionLog =
    InspectionLog(
        id = id,
        inspectionId = inspectionId,
        info = info,
        createdAt = createdAt?.toKtInstant()
    )