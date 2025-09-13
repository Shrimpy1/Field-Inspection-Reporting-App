package com.tiramisu.groupapplication.data.remote.dto

import com.google.firebase.Timestamp

data class InspectionLogDto(
    val id: String = "",
    val createdAt: Timestamp? = null,
    val inspectionId: String = "",
    val info: String = "",
)