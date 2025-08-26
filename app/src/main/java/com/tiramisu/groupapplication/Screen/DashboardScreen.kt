package com.tiramisu.groupapplication.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tiramisu.groupapplication.data.model.InspectionReport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddReport: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    // Collect the list of reports from the ViewModel's Flow
    val reports by viewModel.reports.collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text("Inspection Reports") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddReport) {
                Icon(Icons.Default.Add, contentDescription = "Add New Report")
            }
        }
    ) { paddingValues ->
        // LazyColumn is an efficient way to display a scrollable list
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(reports) { report ->
                ReportListItem(report = report)
            }
        }
    }
}

@Composable
fun ReportListItem(report: InspectionReport) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(text = report.title, style = MaterialTheme.typography.titleMedium)
            Text(text = "Status: ${report.status}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
