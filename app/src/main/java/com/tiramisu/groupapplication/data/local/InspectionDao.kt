package com.tiramisu.groupapplication.data.local

import androidx.room.*
import com.tiramisu.groupapplication.data.model.InspectionReport
import kotlinx.coroutines.flow.Flow

@Dao
interface InspectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateReport(report: InspectionReport)

    @Query("SELECT * FROM inspection_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<InspectionReport>>

    @Query("SELECT * FROM inspection_reports WHERE id = :reportId")
    suspend fun getReportById(reportId: Long): InspectionReport?
}
