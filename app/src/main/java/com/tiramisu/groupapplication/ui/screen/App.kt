package com.tiramisu.groupapplication.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tiramisu.groupapplication.domain.enums.UserRole
import androidx.navigation.navArgument
import com.tiramisu.groupapplication.ui.component.BottomBarConfig
import com.tiramisu.groupapplication.ui.dashboard.DashboardScreen
import com.tiramisu.groupapplication.ui.viewmodel.AppViewModel
import com.tiramisu.groupapplication.ui.viewmodel.InspectionLocationViewModel
import com.tiramisu.groupapplication.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

sealed class Routes(val route: String) {
    object Auth : Routes("auth")
    object Dashboard : Routes("dashboard")
    object Create : Routes("create")
    object Review : Routes("review/{inspectionId}") {
        fun createRoute(inspectionId: String) = "review/$inspectionId"
    }
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
fun App(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    val appVm: AppViewModel = hiltViewModel()
    val topBarState = appVm.topBar.collectAsStateWithLifecycle().value
    val bottomBarState = appVm.bottomBar.collectAsStateWithLifecycle().value
    val fabState = appVm.fab.collectAsStateWithLifecycle().value

    val startDestination = NavBarDestinations.HOME
    var selectedDestination by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }
    val user by mainViewModel.currentUser.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val authRepo = mainViewModel.authRepo

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

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            appVm.reset()
            appVm.setBottomBar(BottomBarConfig(true, defaultNavBar))
        }

        navController.addOnDestinationChangedListener(listener)

        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    Scaffold(
        topBar = {
            if (topBarState.visible) {
                TopAppBar(
                    title = topBarState.title,
                    navigationIcon = topBarState.navIcon,
                    actions = topBarState.actions,
                )
            }
        },
        floatingActionButton = {
            if (fabState.visible) {
                fabState.content()
            }
        },
        bottomBar = {
            if (bottomBarState.visible) {
                bottomBarState.content()
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home.route,
            modifier = Modifier.padding(paddingValues),
            route = "root"
        ) {
            // Dashboard
            composable(route = Routes.Home.route) {
                DashboardScreen(
                    onDetail = { id ->
                        navController.navigate(Routes.InspectionDetails.createRoute(id))
                    },
                    onAdd = { navController.navigate(Routes.InspectionForm.route) },
                    onProvideFab = { config -> appVm.setFab(config) },
                )
            }

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

            // Detail
            composable(
                route = Routes.InspectionDetails.route,
                arguments = listOf(
                    navArgument("inspectionId") { type = NavType.StringType }
                )
            ) {
                InspectionDetailsRoute(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.InspectionEdit.createRoute(id)) },
                    onOpenMap = { id -> navController.navigate(Routes.InspectionMap.createRoute(id)) },
                    onProvideTopBar = { config -> appVm.setTopBar(config) }
                )
            }


            // Create (New)
            composable(Routes.InspectionForm.route) {
                InspectionCreateScreen(
                    onSaved = { newId ->
                        // After creating, navigate to details (or wherever you prefer)
                        navController.navigate(Routes.InspectionDetails.createRoute(newId)) {
                            launchSingleTop = true
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onProvideTopBar = { config -> appVm.setTopBar(config) }
                )
            }

            // Edit (Existing)
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
                        onSaved = { _ ->
                            navController.popBackStack()
                        },
                        onMapNavigate = {
                            navController.navigate(Routes.InspectionMap.createRoute(inspectionId))
                        },
                        onProvideTopBar = { config -> appVm.setTopBar(config) },
                        onBack = { navController.popBackStack() }
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
                LaunchedEffect(Unit) {
                    appVm.setBottomBar(BottomBarConfig(false))
                }

                val vm: InspectionLocationViewModel = hiltViewModel(backStackEntry)

                InspectionLocationScreen(
                    viewModel = vm,
                    onProvideTopBar = { config -> appVm.setTopBar(config) },
                    onBack = { navController.popBackStack() }
                )
                }
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