package com.tiramisu.groupapplication.data.remote.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import kotlin.time.ExperimentalTime

data class InspectionDto @OptIn(ExperimentalTime::class) constructor(
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val type: String = "",
    val title: String = "",
    val notes: String = "",
    val score: String = "",
    val outcome: String = "",
    val status: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)