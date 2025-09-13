package com.tiramisu.groupapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.domain.model.InspectionAttachment
import com.tiramisu.groupapplication.domain.model.InspectionLog
import com.tiramisu.groupapplication.util.Converters

@Database(
    entities = [Inspection::class, InspectionAttachment::class, SyncMeta::class, InspectionLog::class],
    version = 8,
    exportSchema = true
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inspectionDao(): InspectionDao
    abstract fun inspectionAttachmentDao(): InspectionAttachmentDao
    abstract fun syncMetaDao(): SyncMetaDao
    abstract fun logDao(): LogDao
}