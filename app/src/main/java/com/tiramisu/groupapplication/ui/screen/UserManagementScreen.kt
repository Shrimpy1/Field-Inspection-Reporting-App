package com.tiramisu.groupapplication.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tiramisu.groupapplication.domain.enums.UserRole
import com.tiramisu.groupapplication.domain.model.User
import com.tiramisu.groupapplication.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    viewModel: AdminViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val users by viewModel.allUsers.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Users") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users, key = { it.uid }) { user ->
                UserRow(
                    user = user,
                    onChangeRole = { role ->
                        viewModel.changeUserRole(user.uid, role)
                    }
                )
            }
        }
    }
}

@Composable
private fun UserRow(
    user: User,
    onChangeRole: (UserRole) -> Unit
) {
    val currentRole = runCatching { UserRole.valueOf(user.role) }.getOrDefault(UserRole.INSPECTOR)
    var menuExpanded by remember(user.uid) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = user.email.ifBlank { user.uid },
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Role: ${currentRole.name}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(end = 8.dp)
        )

        IconButton(
            onClick = { menuExpanded = true },
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Change role"
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            UserRole.entries.forEach { role ->
                DropdownMenuItem(
                    text = { Text(role.name) },
                    onClick = {
                        menuExpanded = false
                        if (role != currentRole) {
                            onChangeRole(role)
                        }
                    }
                )
            }
        }
    }
}