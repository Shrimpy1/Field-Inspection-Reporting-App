package com.tiramisu.groupapplication.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tiramisu.groupapplication.domain.model.Inspection
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(report: Inspection)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reports: List<Inspection>)

    @Query("SELECT * FROM inspection_reports ORDER BY createdAt DESC")
    fun getAllReports(): Flow<List<Inspection>>

    @Query("SELECT * FROM inspection_reports WHERE id = :reportId LIMIT 1")
    fun getReportById(reportId: String): Flow<Inspection?>

    @Query("DELETE FROM inspection_reports WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE inspection_reports SET deletedAt = :deletedAt, pendingSync = 1 WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)

    @Query("DELETE FROM inspection_reports WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT id FROM inspection_reports")
    suspend fun getAllIds(): List<String>

    @Query("SELECT * FROM inspection_reports WHERE pendingSync = 1")
    suspend fun getPending(): List<Inspection>

    @Query("UPDATE inspection_reports SET pendingSync = 0 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("SELECT * FROM inspection_reports WHERE id = :id LIMIT 1")
    suspend fun getOneImmediate(id: String): Inspection?
}