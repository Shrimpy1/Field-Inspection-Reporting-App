package com.tiramisu.groupapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.util.Converters

@Database(entities = [Inspection::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inspectionDao(): InspectionDao
}