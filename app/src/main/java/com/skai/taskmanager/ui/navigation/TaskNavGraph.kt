package com.skai.taskmanager.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.skai.taskmanager.ui.screens.addedit.AddEditTaskScreen
import com.skai.taskmanager.ui.screens.detail.TaskDetailScreen
import com.skai.taskmanager.ui.screens.history.HistoryScreen
import com.skai.taskmanager.ui.screens.notes.NotesScreen
import com.skai.taskmanager.ui.screens.profile.ProfileScreen
import com.skai.taskmanager.ui.screens.schedule.ScheduleScreen
import com.skai.taskmanager.ui.screens.settings.SettingsScreen
import com.skai.taskmanager.ui.screens.stats.StatsScreen
import com.skai.taskmanager.ui.screens.timetable.TimetableScreen
import com.skai.taskmanager.ui.screens.today.TodayScreen

private const val TRANSITION_MS = 300

@Composable
fun TaskNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Today.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(TRANSITION_MS)) +
                slideIntoContainer(SlideDirection.Start, tween(TRANSITION_MS))
        },
        exitTransition = {
            fadeOut(tween(TRANSITION_MS)) +
                slideOutOfContainer(SlideDirection.Start, tween(TRANSITION_MS))
        },
        popEnterTransition = {
            fadeIn(tween(TRANSITION_MS)) +
                slideIntoContainer(SlideDirection.End, tween(TRANSITION_MS))
        },
        popExitTransition = {
            fadeOut(tween(TRANSITION_MS)) +
                slideOutOfContainer(SlideDirection.End, tween(TRANSITION_MS))
        }
    ) {
        composable(Screen.Today.route) {
            TodayScreen(
                onAddTask = { navController.navigate(Screen.AddEdit.createRoute()) },
                onTaskClick = { id -> navController.navigate(Screen.Detail.createRoute(id)) }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onOpenSchedule = { navController.navigate(Screen.Schedule.route) },
                onOpenTimetable = { navController.navigate(Screen.Timetable.route) },
                onOpenStats = { navController.navigate(Screen.Stats.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Schedule.route) {
            ScheduleScreen(
                onAddTaskForPeriod = { periodId ->
                    navController.navigate(Screen.AddEdit.createRoute(periodId = periodId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Timetable.route) {
            TimetableScreen(
                onAddTask = { navController.navigate(Screen.AddEdit.createRoute(recurring = true)) },
                onTaskClick = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Notes.route) {
            NotesScreen()
        }

        composable(Screen.History.route) {
            HistoryScreen()
        }

        composable(
            route = Screen.AddEdit.route,
            arguments = listOf(
                navArgument(Screen.AddEdit.ARG_TASK_ID) {
                    type = NavType.LongType
                    defaultValue = Screen.AddEdit.NO_TASK_ID
                },
                navArgument(Screen.AddEdit.ARG_PERIOD_ID) {
                    type = NavType.IntType
                    defaultValue = Screen.AddEdit.NO_PERIOD_ID
                },
                navArgument(Screen.AddEdit.ARG_RECURRING) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) {
            AddEditTaskScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument(Screen.Detail.ARG_TASK_ID) { type = NavType.LongType }
            )
        ) {
            TaskDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onEditTask = { id -> navController.navigate(Screen.AddEdit.createRoute(id)) }
            )
        }
    }
}
