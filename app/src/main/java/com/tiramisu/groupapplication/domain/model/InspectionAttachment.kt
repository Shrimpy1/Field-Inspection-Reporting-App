package com.tiramisu.groupapplication.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tiramisu.groupapplication.domain.enums.AttachmentStatus
import kotlin.time.Clock
import kotlin.time.Instant

@Entity(tableName = "inspection_attachments")
data class InspectionAttachment(
    @PrimaryKey val id: String,
    val inspectionId: String,
    val localUri: String?,
    val remoteKey: String?,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAt: Instant = Clock.System.now(),
    val status: AttachmentStatus = AttachmentStatus.PENDING_UPLOAD,
    val lastError: String? = null,
)