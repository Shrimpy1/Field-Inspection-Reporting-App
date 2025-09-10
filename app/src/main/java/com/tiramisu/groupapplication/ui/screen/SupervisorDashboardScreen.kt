package com.tiramisu.groupapplication.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiramisu.groupapplication.domain.model.Inspection
import com.tiramisu.groupapplication.ui.viewmodel.SupervisorViewModel

@Composable
fun SupervisorDashboardScreen(
    viewModel: SupervisorViewModel = hiltViewModel(),
    onReportClick: (inspectionId: String) -> Unit // Add this parameter
) {
    val allReports by viewModel.allReports.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(allReports) { report ->
            SupervisorReportListItem(report = report, onClick = { onReportClick(report.id) })
        }
    }
}

@Composable
private fun SupervisorReportListItem(report: Inspection, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(report.title, style = MaterialTheme.typography.titleMedium)
            Text("Status: ${report.status.name}", style = MaterialTheme.typography.bodyMedium)
            report.reviewedBy?.let {
                Text("Reviewed by: $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

