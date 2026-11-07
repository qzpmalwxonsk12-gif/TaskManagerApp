package com.skai.taskmanager.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.skai.taskmanager.ui.theme.LocalStudioStyle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/** Top-level destinations shown in the bottom navigation bar. */
enum class BottomNavDestination(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
) {
    TODAY(Screen.Today, "Today", Icons.Outlined.Today),
    PROFILE(Screen.Profile, "Profile", Icons.Outlined.Person),
    NOTES(Screen.Notes, "Notes", Icons.Outlined.Description),
    HISTORY(Screen.History, "History", Icons.Outlined.History)
}

/** Routes that should display the bottom navigation bar. */
val bottomNavRoutes: Set<String> = BottomNavDestination.entries.map { it.screen.route }.toSet()

@Composable
fun TaskBottomNavBar(
    navController: NavController,
    currentRoute: String?
) {
    val studio = LocalStudioStyle.current
    NavigationBar(
        modifier = if (studio) Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp)) else Modifier,
        containerColor = if (studio) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface,
        tonalElevation = if (studio) 0.dp else 3.dp
    ) {
        BottomNavDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.screen.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(destination.screen.route) {
                            // Avoid building up a large back stack of tabs.
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}
