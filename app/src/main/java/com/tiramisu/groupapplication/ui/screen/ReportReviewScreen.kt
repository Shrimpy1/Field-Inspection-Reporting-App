package com.tiramisu.groupapplication.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiramisu.groupapplication.ui.viewmodel.ReportReviewViewModel

@Composable
fun ReportReviewScreen(
    viewModel: ReportReviewViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) {
            onNavigateBack()
        }
    }

    Scaffold { padding ->
        if (state.inspection == null) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(padding)) {
                CircularProgressIndicator()
            }
        } else {
            val inspection = state.inspection!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Review Inspection", style = MaterialTheme.typography.headlineSmall)
                HorizontalDivider()

                InspectionDetailItem("Title:", inspection.title)
                InspectionDetailItem("Type:", inspection.type)
                InspectionDetailItem("Notes:", inspection.notes)
                InspectionDetailItem("Score:", inspection.score)
                InspectionDetailItem("Outcome:", inspection.outcome)
                InspectionDetailItem("Status:", inspection.status.name)

                Spacer(modifier = Modifier.height(8.dp))
                Text("Supervisor Actions", style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()

                OutlinedTextField(
                    value = state.reviewNotes,
                    onValueChange = viewModel::onReviewNotesChanged,
                    label = { Text("Review Notes (Required for rejection)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    isError = state.error != null
                )
                if (state.error != null) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = viewModel::rejectReport,
                        enabled = !state.isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reject")
                    }
                    Button(onClick = viewModel::approveReport, enabled = !state.isSaving) {
                        Text("Approve")
                    }
                }
            }
        }
    }
}

@Composable
fun InspectionDetailItem(label: String, value: String) {
    Row {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
        Text(value)
    }
}