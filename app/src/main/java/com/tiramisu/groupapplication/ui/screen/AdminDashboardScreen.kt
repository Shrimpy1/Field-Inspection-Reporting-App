package com.tiramisu.groupapplication.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AdminDashboardScreen(
    onNavigateToLogs: () -> Unit,
    onNavigateToUserList: () -> Unit,
    onNavigateToInspection: () -> Unit
) {
    val contents = listOf(
        AdminDashboardContent(
            title = "Inspections",
            description = "View all inspections.",
            buttonText = "Open Inspection Dashboard",
            onClick = onNavigateToInspection
        ),
        AdminDashboardContent(
            title = "Logs",
            description = "View all inspection logs and export as CSV.",
            buttonText = "Open Logs",
            onClick = onNavigateToLogs
        ),
        AdminDashboardContent(
            title = "User Management",
            description = "View all users and manage their roles.",
            buttonText = "Manage Users",
            onClick = onNavigateToUserList
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        contents.forEach { content ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = content.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = content.description
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = content.onClick) {
                        Text(content.buttonText)
                    }
                }
            }
        }
    }
}

data class AdminDashboardContent(
    val title: String,
    val description: String,
    val buttonText: String,
    val onClick: () -> Unit
)