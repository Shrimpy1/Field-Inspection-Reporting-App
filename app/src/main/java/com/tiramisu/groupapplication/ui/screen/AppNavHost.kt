package com.tiramisu.groupapplication.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tiramisu.groupapplication.ui.dashboard.DashboardScreen

sealed class Routes(val route: String) {
    object Home : Routes("home")
    object Auth : Routes("auth")
    object InspectionDetails : Routes("inspection/{inspectionId}") {
        fun createRoute(inspectionId: String) = "inspection/$inspectionId"
    }

    object InspectionForm : Routes("inspection/new")
    object InspectionMap : Routes("inspection/{inspectionId}/map") {
        fun createRoute(inspectionId: String) = "inspection/$inspectionId/map"
    }

    object InspectionEdit : Routes("inspection/{inspectionId}/edit") {
        fun createRoute(inspectionId: String) = "inspection/$inspectionId/edit"
    }
}

enum class NavBarDestinations(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
) {
    HOME(Routes.Home.route, "Home", Icons.Default.Home, "Home")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val startDestination = NavBarDestinations.HOME
    var selectedDestination by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }

    val defaultNavBar: @Composable () -> Unit = {
        NavigationBar {
            NavBarDestinations.entries.forEachIndexed { index, destination ->
                NavigationBarItem(
                    selected = selectedDestination == index,
                    onClick = {
                        navController.navigate(route = destination.route)
                        selectedDestination = index
                    },
                    icon = {
                        Icon(
                            destination.icon,
                            contentDescription = destination.contentDescription
                        )
                    },
                    label = {
                        Text(
                            destination.label,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                )
            }
        }
    }

    var topBar: @Composable () -> Unit by remember { mutableStateOf({}) }
    var bottomBar: @Composable () -> Unit by remember { mutableStateOf(defaultNavBar) }
    var fab: @Composable () -> Unit by remember { mutableStateOf({}) }

    Scaffold(
        topBar = topBar,
        floatingActionButton = fab
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(route = Routes.Home.route) {
                fab = {
                    FloatingActionButton(onClick = { navController.navigate(Routes.InspectionForm.route) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add New Report")
                    }
                }

                DashboardScreen(onAddReport = { navController.navigate("") })
            }

            composable(route = Routes.InspectionDetails.route) {

            }

            // Create (New)
            composable(Routes.InspectionForm.route) {
                InspectionCreateScreen(
                    onSaved = { newId ->
                        // After creating, navigate to details (or wherever you prefer)
                        navController.navigate("inspection/$newId") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateUp = { navController.navigateUp() }
                )
            }

            // Edit (Existing)
            composable(
                route = Routes.InspectionEdit.route,
                arguments = listOf(
                    navArgument("inspectionId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val inspectionId =
                    backStackEntry.arguments?.getString("inspectionId") ?: return@composable

                InspectionEditScreen(
                    inspectionId = inspectionId,
                    onSaved = { _ ->
                        navController.navigate(Routes.InspectionDetails.createRoute(inspectionId))
                    },
                    onNavigateUp = { navController.navigateUp() },
                    onMapNavigate = {
                        navController.navigate("inspection/$inspectionId/map")
                    }
                )
            }
        }
    }
}