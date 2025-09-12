package com.tiramisu.groupapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tiramisu.groupapplication.domain.enums.AttachmentStatus
import com.tiramisu.groupapplication.domain.model.InspectionAttachment
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionAttachmentDao {
    @Query("SELECT * FROM inspection_attachments WHERE inspectionId = :inspectionId ORDER BY createdAt DESC")
    fun getForInspection(inspectionId: String): Flow<List<InspectionAttachment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<InspectionAttachment>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: InspectionAttachment)

    @Query("UPDATE inspection_attachments SET status = :status, lastError = :error WHERE id = :id")
    suspend fun updateStatus(id: String, status: AttachmentStatus, error: String? = null)

    @Query("UPDATE inspection_attachments SET remoteKey = :remoteKey, status = :status, lastError = NULL WHERE id = :id")
    suspend fun markUploaded(
        id: String,
        remoteKey: String,
        status: AttachmentStatus = AttachmentStatus.SYNCED
    )

    @Query("SELECT * FROM inspection_attachments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): InspectionAttachment?

    @Query("DELETE FROM inspection_attachments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM inspection_attachments WHERE inspectionId = :inspectionId")
    suspend fun countForInspection(inspectionId: String): Int
}