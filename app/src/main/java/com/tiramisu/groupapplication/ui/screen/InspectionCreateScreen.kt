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
import androidx.compose.ui.Modifier
import com.tiramisu.groupapplication.ui.component.InspectionForm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionCreateScreen(
    onSaved: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onMapNavigate: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Inspection") },
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
            onSaved = onSaved,
            onMapNavigate = onMapNavigate
        )
    }
}