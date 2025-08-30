package com.tiramisu.groupapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inspection_reports")
data class InspectionReport(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String,
    val status: InspectionStatus,
    val timestamp: Long,
    val gpsLatitude: Double?,
    val gpsLongitude: Double?,
    val isSynced: Boolean = false
)

enum class InspectionStatus {
    PASSED, FAILED, PENDING_REVIEW, NEEDS_ATTENTION
}
