package com.tiramisu.groupapplication.ui.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ExportLogsButton(onExport: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> if (uri != null) onExport(uri) }

    Button(onClick = {
        val name = "logs_${java.time.LocalDateTime.now()}.csv"
        launcher.launch(name)
    }) { Text("Export CSV") }
}