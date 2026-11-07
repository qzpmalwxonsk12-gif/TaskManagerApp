package com.skai.taskmanager.ui.navigation

/**
 * Type-safe-ish route definitions for the app's navigation graph.
 */
sealed class Screen(val route: String) {

    data object Today : Screen("today")

    data object Profile : Screen("profile")

    data object Schedule : Screen("schedule")

    data object Timetable : Screen("timetable")

    data object Stats : Screen("stats")

    data object Settings : Screen("settings")

    data object Notes : Screen("notes")

    data object History : Screen("history")

    data object AddEdit : Screen("add_edit?taskId={taskId}&periodId={periodId}&recurring={recurring}") {
        const val ARG_TASK_ID = "taskId"
        const val ARG_PERIOD_ID = "periodId"
        const val ARG_RECURRING = "recurring"
        const val NO_TASK_ID = -1L
        const val NO_PERIOD_ID = -1

        /**
         * Pass no id (or [NO_TASK_ID]) to add a new task; pass a real id to edit. Optionally
         * pre-assign the new task to a [periodId], and/or start it as [recurring] (Timetable).
         */
        fun createRoute(
            taskId: Long = NO_TASK_ID,
            periodId: Int = NO_PERIOD_ID,
            recurring: Boolean = false
        ): String = "add_edit?taskId=$taskId&periodId=$periodId&recurring=$recurring"
    }

    data object Detail : Screen("detail/{taskId}") {
        const val ARG_TASK_ID = "taskId"

        fun createRoute(taskId: Long): String = "detail/$taskId"
    }
}
