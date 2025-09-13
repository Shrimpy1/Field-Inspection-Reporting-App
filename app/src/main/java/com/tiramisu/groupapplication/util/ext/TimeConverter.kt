package com.tiramisu.groupapplication.util.ext

import com.google.firebase.Timestamp
import java.util.Date
import kotlin.time.Instant

fun Timestamp.toKtInstant(): Instant =
    this.toDate().time.let { Instant.fromEpochMilliseconds(it) }

fun Instant.toTimestamp(): Timestamp =
    Timestamp(Date(this.toEpochMilliseconds()))