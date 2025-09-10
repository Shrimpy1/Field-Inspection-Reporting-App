package com.tiramisu.groupapplication.ui.screen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.tiramisu.groupapplication.ui.component.InspectionForm
import com.tiramisu.groupapplication.ui.component.TopBarConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionCreateScreen(
    onSaved: (String) -> Unit,
    onBack: () -> Unit,
    onMapNavigate: () -> Unit = {},
    onProvideTopBar: (TopBarConfig) -> Unit,
) {
    InspectionForm(
        onSaved = onSaved,
        onMapNavigate = onMapNavigate,
        onProvideTopBar = onProvideTopBar,
        onBack = onBack,
        screenTitle = "Create Inspection"
    )
}