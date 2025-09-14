package com.tiramisu.groupapplication.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tiramisu.groupapplication.domain.enums.InspectionStatus
import com.tiramisu.groupapplication.domain.enums.InspectionType
import com.tiramisu.groupapplication.util.ext.clickableNoIndication
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun Filter(
    filter: InspectionFilter,
    onClickType: () -> Unit,
    onClickDate: () -> Unit,
    onClear: () -> Unit,
    showStatus: Boolean = false,
    onClickStatus: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Type chip
        FilterChipLike(
            label = "Type",
            value = filter.type?.display ?: "All",
            onClick = onClickType
        )
        // Date chip
        FilterChipLike(
            label = "Date",
            value = buildDateLabel(filter.startMillis, filter.endMillis),
            onClick = onClickDate
        )// Status chip
        if (showStatus) {
            FilterChipLike(
                label = "Status",
                value = filter.status?.name ?: "All",
                onClick = onClickStatus
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (filter.type != null || filter.startMillis != null || filter.endMillis != null) {
            Text(
                text = "Clear",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .then(Modifier)
                    .clickableNoIndication(onClick = onClear)
            )
        }
    }
}

@Composable
private fun FilterChipLike(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.large
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickableNoIndication(onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun buildDateLabel(start: Long?, end: Long?): String {
    fun fmt(m: Long) = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(java.util.Date(m))
    return when {
        start != null && end != null -> "${fmt(start)} – ${fmt(end)}"
        start != null -> "From ${fmt(start)}"
        end != null -> "Until ${fmt(end)}"
        else -> "Any"
    }
}

data class InspectionFilter(
    val type: InspectionType? = null,
    val startMillis: Long? = null,
    val endMillis: Long? = null,
    val status: InspectionStatus? = null,
)