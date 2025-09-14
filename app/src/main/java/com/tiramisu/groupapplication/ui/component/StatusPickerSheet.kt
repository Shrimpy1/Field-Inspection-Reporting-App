package com.tiramisu.groupapplication.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tiramisu.groupapplication.domain.enums.InspectionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusPickerSheet(
    selected: InspectionStatus?,
    onDismiss: () -> Unit,
    onSelect: (InspectionStatus) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            items(
                items = InspectionStatus.entries.toList(),
                key = { it.name }
            ) { type ->
                ListItem(
                    headlineContent = {
                        Text(
                            type.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() })
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(type) },
                    trailingContent = {
                        if (type == selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        }
    }
}