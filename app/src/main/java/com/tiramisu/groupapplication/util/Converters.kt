package com.tiramisu.groupapplication.util

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.TypeConverter
import com.tiramisu.groupapplication.domain.enums.InspectionStatus
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class Converters {
    /**
     * Converts a list of Uri objects into a single comma-separated String.
     */
    @TypeConverter
    fun fromUriList(uriList: List<Uri>): String {
        return uriList.joinToString(",") { it.toString() }
    }

    /**
     * Converts a comma-separated String back into a list of Uri objects.
     */
    @TypeConverter
    fun toUriList(data: String): List<Uri> {
        // Return an empty list if the stored string is empty
        if (data.isEmpty()) {
            return emptyList()
        }
        return data.split(",").map { it.toUri() }
    }

    @TypeConverter
    fun instantToLong(time: Instant?): Long? = time?.toEpochMilliseconds()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = Instant.Companion.fromEpochMilliseconds(value ?: 0)

    @TypeConverter
    fun statusToString(v: InspectionStatus?): String? = v?.name

    @TypeConverter
    fun stringToStatus(s: String?): InspectionStatus? =
        s?.let { InspectionStatus.valueOf(it) }
}