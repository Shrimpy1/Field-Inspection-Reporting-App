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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tiramisu.groupapplication.domain.enums.InspectionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypePickerSheet(
    selected: InspectionType?,
    onDismiss: () -> Unit,
    onSelect: (InspectionType) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        val grouped = remember { InspectionType.entries.groupBy { it.category } }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            grouped.forEach { (category, types) ->
                item("header-$category") {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                items(
                    items = types,
                    key = { it.name }
                ) { type ->
                    ListItem(
                        headlineContent = { Text(type.display) },
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
}