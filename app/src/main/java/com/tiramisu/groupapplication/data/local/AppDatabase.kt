package com.tiramisu.groupapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tiramisu.groupapplication.domain.model.Inspection

@Database(entities = [Inspection::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inspectionDao(): InspectionDao
}