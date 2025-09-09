package com.tiramisu.groupapplication.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tiramisu.groupapplication.domain.enums.UserRole
import com.tiramisu.groupapplication.ui.dashboard.DashboardScreen
import com.tiramisu.groupapplication.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

sealed class Routes(val route: String) {
    object Auth : Routes("auth")
    object Dashboard : Routes("dashboard")
    object Create : Routes("create")
    object Review : Routes("review/{inspectionId}") {
        fun createRoute(inspectionId: String) = "review/$inspectionId"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val user by mainViewModel.currentUser.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val authRepo = mainViewModel.authRepo

    val startDestination = if (user == null) Routes.Auth.route else Routes.Dashboard.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.Auth.route) {
            AuthScreen()
        }

        composable(Routes.Dashboard.route) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Dashboard (${user?.role})") },
                        actions = {
                            IconButton(onClick = {
                                coroutineScope.launch { authRepo.signOut() }
                            }) {
                                Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out")
                            }
                        }
                    )
                },
                floatingActionButton = {
                    // Only show the FAB for Inspectors
                    if (user?.role == UserRole.INSPECTOR.name) {
                        FloatingActionButton(onClick = { navController.navigate(Routes.Create.route) }) {
                            Icon(Icons.Default.Add, contentDescription = "Add New Report")
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (user?.role) {
                        UserRole.INSPECTOR.name -> DashboardScreen(onInspectionClick = { /* Define action if needed */ })
                        UserRole.SUPERVISOR.name -> SupervisorDashboardScreen(onReportClick = { inspectionId ->
                            navController.navigate(Routes.Review.createRoute(inspectionId))
                        })
                        UserRole.ADMIN.name -> AdminDashboardScreen()
                    }
                }
            }
        }

        composable(Routes.Create.route) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("New Inspection") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    InspectionForm(onSaved = {
                        navController.popBackStack()
                    })
                }
            }
        }

        composable(Routes.Review.route) {
            ReportReviewScreen(onNavigateBack = { navController.popBackStack() })
        }
    }

    LaunchedEffect(user, navController.currentBackStackEntry) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (user == null && currentRoute != Routes.Auth.route) {
            navController.navigate(Routes.Auth.route) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        } else if (user != null && currentRoute == Routes.Auth.route) {
            navController.navigate(Routes.Dashboard.route) {
                popUpTo(Routes.Auth.route) { inclusive = true }
            }
        }
    }
}

@Composable
fun InspectionForm(onSaved: () -> Boolean) {
    TODO("Not yet implemented")
}

