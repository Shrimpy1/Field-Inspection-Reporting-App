package com.tiramisu.groupapplication.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    viewModel: SupervisorViewModel = hiltViewModel()
) {
    val reports by viewModel.allReports.collectAsStateWithLifecycle()

    if (reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("No inspection reports available for review.", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(reports) { report ->
                InspectionReviewCard(report = report)
            }
        }
    }
}

@Composable
private fun InspectionReviewCard(report: Inspection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(report.title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Status: ${report.status.name}", style = MaterialTheme.typography.bodyMedium)
            Text("Outcome: ${report.outcome}", style = MaterialTheme.typography.bodyMedium)
            // TODO: Add buttons for Approve/Reject and adding notes
        }
    }
}
