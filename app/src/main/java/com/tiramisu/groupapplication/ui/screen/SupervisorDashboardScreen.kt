package com.tiramisu.groupapplication.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiramisu.groupapplication.ui.component.DateRangeSheet
import com.tiramisu.groupapplication.ui.component.Filter
import com.tiramisu.groupapplication.ui.component.InspectionFilter
import com.tiramisu.groupapplication.ui.component.InspectionListItem
import com.tiramisu.groupapplication.ui.component.SearchTextField
import com.tiramisu.groupapplication.ui.component.StatusPickerSheet
import com.tiramisu.groupapplication.ui.component.TypePickerSheet
import com.tiramisu.groupapplication.ui.viewmodel.SupervisorViewModel
import com.tiramisu.groupapplication.util.ext.applyFilter
import com.tiramisu.groupapplication.util.ext.filterByTitleQuery

@Composable
fun SupervisorDashboardScreen(
    viewModel: SupervisorViewModel = hiltViewModel(),
    onReportClick: (inspectionId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var filter by remember { mutableStateOf(InspectionFilter()) }
    var showTypeSheet by remember { mutableStateOf(false) }
    var showDateSheet by remember { mutableStateOf(false) }
    var showStatusSheet by remember { mutableStateOf(false) }

    var query by remember { mutableStateOf("") }

    val filteredItems = remember(uiState.items, filter) {
        uiState.items.applyFilter(filter).filterByTitleQuery(query)
    }

    var showFilter by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchTextField(
                    modifier = Modifier.weight(1f),
                    query = query,
                    onQueryChange = { query = it },
                    onClear = { query = "" }
                )

                OutlinedIconButton(
                    onClick = { showFilter = !showFilter }
                ) {
                    Icon(Icons.Filled.FilterList, contentDescription = "Filter")
                }
            }
        }

        if (showFilter) {
            item {
                Filter(
                    filter = filter,
                    onClickType = { showTypeSheet = true },
                    onClickDate = { showDateSheet = true },
                    onClear = { filter = InspectionFilter() },
                    showStatus = true,
                    onClickStatus = { showStatusSheet = true }
                )
            }
        }

        items(filteredItems) { report ->
            InspectionListItem(
                report = report,
                onClick = {
                    if (report.id.isNotBlank()) {
                        onReportClick(report.id)
                    }
                }
            )
        }
    }

    if (showTypeSheet) {
        TypePickerSheet(
            selected = filter.type,
            onDismiss = { showTypeSheet = false },
            onSelect = { picked ->
                filter = filter.copy(type = picked)
                showTypeSheet = false
            },
        )
    }
    if (showDateSheet) {
        DateRangeSheet(
            startMillis = filter.startMillis,
            endMillis = filter.endMillis,
            onDismiss = { showDateSheet = false },
            onApply = { start, end ->
                filter = filter.copy(startMillis = start, endMillis = end)
                showDateSheet = false
            },
            onClear = {
                filter = filter.copy(startMillis = null, endMillis = null)
                showDateSheet = false
            }
        )
    }
    if (showStatusSheet) {
        StatusPickerSheet(
            selected = filter.status,
            onDismiss = { showStatusSheet = false },
            onSelect = { picked ->
                filter = filter.copy(status = picked)
                showStatusSheet = false
            }
        )
    }
}