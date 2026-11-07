package com.skai.taskmanager.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.skai.taskmanager.ui.navigation.TaskBottomNavBar
import com.skai.taskmanager.ui.navigation.TaskNavGraph
import com.skai.taskmanager.ui.navigation.bottomNavRoutes

/**
 * Root composable: owns the [NavController], renders the bottom navigation bar on the
 * top-level destinations, and hosts the navigation graph.
 */
@Composable
fun TaskManagerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                TaskBottomNavBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        TaskNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
