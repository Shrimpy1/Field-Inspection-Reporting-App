package com.tiramisu.groupapplication.domain.repository

import android.net.Uri
import com.tiramisu.groupapplication.domain.model.InspectionAttachment
import kotlinx.coroutines.flow.Flow

interface AttachmentRepository {
    fun getAttachments(inspectionId: String): Flow<List<InspectionAttachment>>
    suspend fun addAttachment(inspectionId: String, uri: Uri): Any
    suspend fun retryUpload(attachmentId: String)
    suspend fun deleteAttachment(attachmentId: String)
    suspend fun hydrateFromRemoteIfEmpty(inspectionId: String)
    suspend fun uploadOne(attachmentId: String)
}