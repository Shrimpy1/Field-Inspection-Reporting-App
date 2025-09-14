package com.tiramisu.groupapplication.ui.screen

sealed class Routes(val route: String) {
    object Auth : Routes("auth")
    object Dashboard : Routes("dashboard")
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

    object Misc : Routes("misc")

    object Logs : Routes("logs")
    object UserList : Routes("users")
    object Map : Routes("map")
}