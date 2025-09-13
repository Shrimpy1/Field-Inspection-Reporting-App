package com.tiramisu.groupapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tiramisu.groupapplication.domain.model.InspectionLog
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

@Dao
interface LogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(e: InspectionLog)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(list: List<InspectionLog>)

    @Query("SELECT * FROM inspection_logs ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getBatch(limit: Int = 100): List<InspectionLog>

    @Query("DELETE FROM inspection_logs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM inspection_logs WHERE createdAt < :olderThan")
    suspend fun filterOlderThan(olderThan: Instant)

    @Query("SELECT * FROM inspection_logs WHERE pendingSync = 1")
    suspend fun getPending(): List<InspectionLog>

    @Query("UPDATE inspection_logs SET pendingSync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("SELECT * FROM inspection_logs ORDER BY createdAt DESC")
    fun getAllLogs(): Flow<List<InspectionLog>>

    @Query(
        """
        SELECT id, createdAt, inspectionId, info
        FROM inspection_logs
        WHERE (:inspectionId IS NULL OR inspectionId = :inspectionId)
          AND (:since IS NULL OR createdAt >= :since)
          AND (:until IS NULL OR createdAt <  :until)
        ORDER BY createdAt ASC
    """
    )
    fun exportCursor(
        inspectionId: String? = null,
        since: Long? = null,
        until: Long? = null
    ): android.database.Cursor
}