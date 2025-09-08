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
    suspend fun getReportById(reportId: String): Flow<Inspection?>

    @Query("DELETE FROM inspection_reports WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}