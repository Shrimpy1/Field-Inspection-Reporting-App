package com.tiramisu.groupapplication.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.tiramisu.groupapplication.ui.component.InspectionForm
import com.tiramisu.groupapplication.ui.viewmodel.InspectionFormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionEditScreen(
    inspectionId: String,
    onSaved: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onMapNavigate: () -> Unit
) {
    val viewModel: InspectionFormViewModel = hiltViewModel()

    LaunchedEffect(inspectionId) {
        viewModel.loadForEdit(inspectionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Inspection") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        InspectionForm(
            modifier = Modifier.padding(padding),
            viewModel = viewModel,
            onSaved = onSaved,
            onMapNavigate = onMapNavigate
        )
    }
}