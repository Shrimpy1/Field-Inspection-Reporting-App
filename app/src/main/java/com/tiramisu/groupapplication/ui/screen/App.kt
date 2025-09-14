package com.tiramisu.groupapplication.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tiramisu.groupapplication.domain.enums.UserRole
import com.tiramisu.groupapplication.domain.model.User
import com.tiramisu.groupapplication.ui.component.ConnectivityToasts
import com.tiramisu.groupapplication.ui.component.NavBar
import com.tiramisu.groupapplication.ui.viewmodel.AppViewModel
import com.tiramisu.groupapplication.ui.viewmodel.SessionState
import com.tiramisu.groupapplication.ui.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    sessionViewModel: SessionViewModel = hiltViewModel(),
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val state by sessionViewModel.sessionState.collectAsStateWithLifecycle()

    when (state) {
        SessionState.Loading -> {} // simple progress or branded splash
        SessionState.SignedOut -> AuthNavHost() // contains only Auth routes + its own navController
        is SessionState.SignedIn -> AppNavHost(
            user = (state as SessionState.SignedIn).user,
            onLogout = { sessionViewModel.signOut() }
        ) // contains Dashboard etc. + its own navController
    }

    ConnectivityToasts(viewModel = appViewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    user: User,
    navController: NavHostController = rememberNavController(),
    onLogout: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Dashboard.route,
    ) {
        // Auth
        composable(Routes.Auth.route) {
            AuthScreen()
        }

        // Dashboard
        composable(Routes.Dashboard.route) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "Dashboard (${user.role})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                    )
                },
                floatingActionButton = {
                    if (user.role == UserRole.INSPECTOR) {
                        FloatingActionButton(onClick = { navController.navigate(Routes.InspectionForm.route) }) {
                            Icon(Icons.Default.Add, contentDescription = "Add New Report")
                        }
                    }
                },
                bottomBar = { NavBar(navController = navController) }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (user.role) {
                        UserRole.INSPECTOR -> MainDashboard(
                            onDetail = { id ->
                                navController.navigate(Routes.InspectionDetails.createRoute(id))
                            },
                        )

                        UserRole.SUPERVISOR -> SupervisorDashboardScreen(onReportClick = { inspectionId ->
                            navController.navigate(Routes.Review.createRoute(inspectionId))
                        })

                        UserRole.ADMIN -> AdminDashboardScreen(
                            onNavigateToLogs = { navController.navigate(Routes.Logs.route) },
                            onNavigateToUserList = { navController.navigate(Routes.UserList.route) },
                            onNavigateToInspection = { navController.navigate(Routes.AdminDashboard.route) }
                        )
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
                navController = navController,
                user = user
            )
        }


        // Create
        composable(Routes.InspectionForm.route) {
            InspectionCreateRoute(
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
                InspectionEditRoute(
                    inspectionId = inspectionId,
                    navController = navController
                )
            }
        }

        composable(Routes.Review.route) {
            InspectionReviewScreen(
                onNavigateBack = { navController.popBackStack() },
                navController = navController
            )
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

        // Misc
        composable(Routes.Misc.route) {
            MiscScreen(
                user = user,
                navController = navController,
                onLogout = onLogout
            )
        }

        // Logs
        composable(Routes.Logs.route) {
            LogsScreen(
                onBack = { navController.popBackStack() },
                navController = navController
            )
        }

        // User List
        composable(Routes.UserList.route) {
            UserListScreen(
                onBack = { navController.popBackStack() },
                navController = navController
            )
        }

        // Map with all inspections
        composable(Routes.Map.route) {
            InspectionsMapScreen(
                onBack = { navController.popBackStack() },
                onNavigateInspection = { id ->
                    navController.navigate(
                        if (user.role == UserRole.SUPERVISOR)
                            Routes.Review.createRoute(id)
                        else
                            Routes.InspectionDetails.createRoute(id)
                    )
                }
            )
        }

        // Admin dashboard
        composable(Routes.AdminDashboard.route) {
            AdminInspectionRoute(navController = navController)
        }
    }
}

@Composable
fun AuthNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Auth.route,
    ) {
        // Auth
        composable(Routes.Auth.route) {
            AuthScreen()
        }
    }
}