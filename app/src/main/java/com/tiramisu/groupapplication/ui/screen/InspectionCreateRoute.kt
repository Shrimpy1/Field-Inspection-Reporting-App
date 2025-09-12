package com.tiramisu.groupapplication.ui.screen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionCreateRoute(
    navController: NavHostController
) {
    InspectionForm(
        onSaved = { newId ->
            // After creating, navigate to details (or wherever you prefer)
            navController.navigate(Routes.InspectionDetails.createRoute(newId)) {
                launchSingleTop = true
            }
        },
        onBack = { navController.popBackStack() },
        screenTitle = "Create Inspection",
        navController = navController
    )
}