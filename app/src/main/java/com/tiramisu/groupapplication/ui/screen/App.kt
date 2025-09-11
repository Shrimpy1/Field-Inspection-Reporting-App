package com.tiramisu.groupapplication.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tiramisu.groupapplication.domain.enums.UserRole
import com.tiramisu.groupapplication.ui.component.BottomBarConfig
import com.tiramisu.groupapplication.ui.component.NavBar
import com.tiramisu.groupapplication.ui.viewmodel.AppViewModel
import com.tiramisu.groupapplication.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    mainViewModel: MainViewModel = hiltViewModel(),
    appVm: AppViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    val user by mainViewModel.currentUser.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val authRepo = mainViewModel.authRepo

    NavHost(
        navController = navController,
        startDestination = Routes.Dashboard.route,
        route = "root"
    ) {
        // Auth
        composable(Routes.Auth.route) {
            LaunchedEffect(Unit) {
                appVm.setBottomBar(BottomBarConfig())
            }

            AuthScreen()
        }

        // Dashboard
        composable(Routes.Dashboard.route) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Dashboard (${user?.role})") },
                        actions = {
                            IconButton(onClick = {
                                coroutineScope.launch { authRepo.signOut() }
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "Sign Out"
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {
                    if (user?.role == UserRole.INSPECTOR.name) {
                        FloatingActionButton(onClick = { navController.navigate(Routes.InspectionForm.route) }) {
                            Icon(Icons.Default.Add, contentDescription = "Add New Report")
                        }
                    }
                },
                bottomBar = { NavBar(navController = navController) }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (user?.role) {
                        UserRole.INSPECTOR.name -> DashboardScreen(
                            onDetail = { id ->
                                navController.navigate(Routes.InspectionDetails.createRoute(id))
                            },
                        )

                        UserRole.SUPERVISOR.name -> SupervisorDashboardScreen(onReportClick = { inspectionId ->
                            navController.navigate(Routes.Review.createRoute(inspectionId))
                        })

                        UserRole.ADMIN.name -> AdminDashboardScreen()
                    }
                }
            }
        }

        // Detail
        composable(
            route = Routes.InspectionDetails.route,
            arguments = listOf(
                navArgument("inspectionId") { type = NavType.StringType }
            )
        ) {
            InspectionDetailsRoute(
                navController = navController
            )
        }


        // Create
        composable(Routes.InspectionForm.route) {
            InspectionCreateScreen(
                navController = navController,
            )
        }

        // Edit
        composable(
            route = Routes.InspectionEdit.route,
            arguments = listOf(
                navArgument("inspectionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val inspectionId = backStackEntry.arguments?.getString("inspectionId")
            if (inspectionId.isNullOrBlank()) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                InspectionEditScreen(
                    inspectionId = inspectionId,
                    navController = navController
                )
            }
        }

        composable(Routes.Review.route) {
            ReportReviewScreen(onNavigateBack = { navController.popBackStack() })
        }

        // Map view
        composable(
            route = Routes.InspectionMap.route,
            arguments = listOf(
                navArgument("inspectionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            InspectionLocationScreen(
                onBack = { navController.popBackStack() }
            )
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