package com.tiramisu.groupapplication.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tiramisu.groupapplication.util.ext.clickableNoIndication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSheet(
    startMillis: Long?,
    endMillis: Long?,
    onDismiss: () -> Unit,
    onApply: (start: Long?, end: Long?) -> Unit,
    onClear: () -> Unit
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = startMillis,
        initialSelectedEndDateMillis = endMillis
    )
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Clear",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickableNoIndication(onClick = onClear)
                )
                Text(
                    "Apply",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickableNoIndication {
                        onApply(state.selectedStartDateMillis, state.selectedEndDateMillis)
                    }
                )
            }

            DateRangePicker(
                state = state,
                showModeToggle = false
            )
        }
    }
}