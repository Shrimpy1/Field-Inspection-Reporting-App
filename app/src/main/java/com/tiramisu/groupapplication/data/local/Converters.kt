package com.tiramisu.groupapplication.data.local

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.TypeConverter

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
}
