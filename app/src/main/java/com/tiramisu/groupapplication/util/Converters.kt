package com.tiramisu.groupapplication.util

import androidx.room.TypeConverter
import com.tiramisu.groupapplication.domain.enums.InspectionStatus
import com.tiramisu.groupapplication.domain.enums.InspectionType
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class Converters {
    @TypeConverter
    fun instantToLong(time: Instant?): Long? = time?.toEpochMilliseconds()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = Instant.Companion.fromEpochMilliseconds(value ?: 0)

    @TypeConverter
    fun statusToString(v: InspectionStatus?): String? = v?.name

    @TypeConverter
    fun stringToStatus(s: String?): InspectionStatus? =
        s?.let { InspectionStatus.valueOf(it) }

    @TypeConverter
    fun typeToString(v: InspectionType?): String? = v?.name

    @TypeConverter
    fun stringToType(s: String?): InspectionType? =
        s?.let { InspectionType.valueOf(it) }
}