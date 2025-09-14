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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.tiramisu.groupapplication.domain.enums.InspectionStatus
import com.tiramisu.groupapplication.ui.component.NavBar
import com.tiramisu.groupapplication.ui.viewmodel.ReportReviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionReviewScreen(
    viewModel: ReportReviewViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Review Inspection",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = { NavBar(navController = navController) }
    ) { padding ->
        if (state.inspection == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
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
                InspectionDetailItem("Title:", inspection.title)
                InspectionDetailItem("Type:", inspection.type.display)
                InspectionDetailItem("Notes:", inspection.notes)
                InspectionDetailItem("Score:", inspection.score)
                InspectionDetailItem("Outcome:", inspection.outcome)
                InspectionDetailItem("Status:", inspection.status.name)

                if (state.inspection!!.status == InspectionStatus.PENDING_REVIEW || state.inspection!!.status == InspectionStatus.NEEDS_ATTENTION) {
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


                    if (state.inspection!!.status != InspectionStatus.NEEDS_ATTENTION) {
                        OutlinedButton(
                            onClick = viewModel::markNeedsAttention,
                            enabled = !state.isSaving,
                        ) {
                            Text(
                                "Mark Needs Attention",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        Button(
                            onClick = viewModel::rejectReport,
                            enabled = !state.isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reject")
                        }
                        Button(
                            onClick = viewModel::approveReport,
                            enabled = !state.isSaving,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Approve")
                        }
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