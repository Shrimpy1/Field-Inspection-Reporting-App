package com.tiramisu.groupapplication.ui.screen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.tiramisu.groupapplication.ui.component.InspectionForm
import com.tiramisu.groupapplication.ui.viewmodel.InspectionFormViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionEditScreen(
    inspectionId: String,
    viewModel: InspectionFormViewModel = hiltViewModel(),
    navController: NavHostController
) {
    LaunchedEffect(inspectionId) {
        viewModel.loadForEdit(inspectionId)
    }

    InspectionForm(
        viewModel = viewModel,
        onSaved = { _ ->
            navController.popBackStack()
        },
        onBack = { navController.popBackStack() },
        screenTitle = "Edit Inspection",
        navController = navController
    )
}