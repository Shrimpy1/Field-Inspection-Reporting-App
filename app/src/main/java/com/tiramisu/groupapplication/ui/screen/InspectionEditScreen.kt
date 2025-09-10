package com.tiramisu.groupapplication.ui.screen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.tiramisu.groupapplication.ui.component.InspectionForm
import com.tiramisu.groupapplication.ui.component.TopBarConfig
import com.tiramisu.groupapplication.ui.viewmodel.InspectionFormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionEditScreen(
    inspectionId: String,
    onSaved: (String) -> Unit,
    onMapNavigate: () -> Unit,
    viewModel: InspectionFormViewModel = hiltViewModel(),
    onProvideTopBar: (TopBarConfig) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(inspectionId) {
        viewModel.loadForEdit(inspectionId)
    }

    InspectionForm(
        viewModel = viewModel,
        onSaved = onSaved,
        onMapNavigate = onMapNavigate,
        onProvideTopBar = onProvideTopBar,
        onBack = onBack,
        screenTitle = "Edit Inspection"
    )
}