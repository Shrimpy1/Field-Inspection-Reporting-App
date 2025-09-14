package com.tiramisu.groupapplication.util.ext

import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.ui.component.InspectionFilter
import kotlin.time.Instant

fun List<Inspection>.applyFilter(f: InspectionFilter): List<Inspection> {
    return this.filter { ins ->
        val typeOk = f.type == null || ins.type == f.type
        val startOk =
            f.startMillis?.let { ins.createdAt >= Instant.fromEpochMilliseconds(it) } ?: true
        val endOk = f.endMillis?.let { ins.createdAt <= Instant.fromEpochMilliseconds(it) } ?: true
        val statusOk = f.status == null || ins.status == f.status
        typeOk && startOk && endOk && statusOk
    }
}

fun List<Inspection>.filterByTitleQuery(
    query: String
): List<Inspection> {
    val q = query.trim()
    if (q.isEmpty()) return this
    return filter { it.title.contains(q, ignoreCase = true) }
}