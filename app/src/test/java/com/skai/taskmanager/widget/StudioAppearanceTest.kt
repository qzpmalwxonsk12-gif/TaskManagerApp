package com.skai.taskmanager.widget

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.skai.taskmanager.domain.model.Task
import com.skai.taskmanager.ui.components.ProgressCard
import com.skai.taskmanager.ui.components.TaskListItem
import com.skai.taskmanager.ui.navigation.TaskBottomNavBar
import com.skai.taskmanager.ui.screens.settings.AppearanceSettingsContent
import com.skai.taskmanager.ui.theme.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "w400dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StudioAppearanceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun appSwitchesBetweenBothStylesWithoutLosingTaskContent() {
        AppearancePreferences.setApp(context, AppStyle.STUDIO)
        val dark = mutableStateOf(false)
        compose.setContent {
            TaskManagerTheme(darkTheme = dark.value) {
                Surface(color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Your daily space", style = MaterialTheme.typography.headlineMedium)
                    Text("A quieter way to get things done.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ProgressCard(1, 3)
                    Text("ON YOUR LIST", style = MaterialTheme.typography.labelMedium)
                    TaskListItem(Task(id = 1, title = "Make something lovely", description = "A little progress is still progress."), {}, {})
                    TaskListItem(Task(id = 2, title = "A moment to read", timeMinutes = 960), {}, {})
                    TaskListItem(Task(id = 3, title = "Water the plants", isCompleted = true), {}, {})
                    Spacer(Modifier.weight(1f))
                    TaskBottomNavBar(rememberNavController(), "today")
                }
                }
            }
        }
        compose.onNodeWithText("Make room\nfor what matters.").assertExists()
        save("app-studio-light")
        compose.runOnIdle { dark.value = true }
        save("app-studio-dark")
        compose.runOnIdle { dark.value = false; AppearancePreferences.setApp(context, AppStyle.CLASSIC) }
        compose.onNodeWithText("1 of 3 tasks completed").assertExists()
        compose.onNodeWithText("Make something lovely").assertExists()
        compose.onNodeWithText("Make room\nfor what matters.").assertDoesNotExist()
        save("app-original")
    }

    @Test fun settingsSelectorsApplyAppAndWidgetChoices() {
        AppearancePreferences.setApp(context, AppStyle.STUDIO)
        AppearancePreferences.setWidget(context, WidgetStyle.PAPER)
        compose.setContent {
            val appearance by AppearancePreferences.state.collectAsState()
            TaskManagerTheme(darkTheme = false) {
                Surface {
                    AppearanceSettingsContent(appearance,
                        { AppearancePreferences.setApp(context, it) },
                        { AppearancePreferences.setWidget(context, it) })
                }
            }
        }
        compose.onNodeWithText("Neon Night").performClick()
        compose.runOnIdle { assertEquals(WidgetStyle.NEON, AppearancePreferences.read(context).widget) }
        compose.onNodeWithText("Original").performClick()
        compose.runOnIdle { assertEquals(AppStyle.CLASSIC, AppearancePreferences.read(context).app) }
        compose.onNodeWithText("Studio").performClick()
        compose.onNodeWithText("Paper Garden").performClick()
        save("appearance-settings")
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.runOnIdle { view.draw(Canvas(bitmap)) }
        val file = File("build/appearance-previews/$name.png")
        requireNotNull(file.parentFile).mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
