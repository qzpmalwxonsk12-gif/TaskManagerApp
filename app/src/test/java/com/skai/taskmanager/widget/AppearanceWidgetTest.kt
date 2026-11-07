package com.skai.taskmanager.widget

import android.app.Application
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.skai.taskmanager.R
import com.skai.taskmanager.domain.model.Note
import com.skai.taskmanager.domain.model.Task
import com.skai.taskmanager.ui.theme.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppearanceWidgetTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val tasks get() = listOf(
        Task(id = 1, title = "Make something lovely"),
        Task(id = 2, title = "A moment to read", timeMinutes = 960),
        Task(id = 3, title = "Water the plants", isCompleted = true, completedAt = System.currentTimeMillis())
    )

    @Test fun appearancePersistsAndChoicesAreIndependent() {
        context.getSharedPreferences("appearance", 0).edit().clear().commit()
        AppearancePreferences.initialize(context)
        assertEquals(Appearance(), AppearancePreferences.state.value)
        AppearancePreferences.setApp(context, AppStyle.CLASSIC)
        AppearancePreferences.setWidget(context, WidgetStyle.NEON)
        AppearancePreferences.initialize(context)
        assertEquals(Appearance(AppStyle.CLASSIC, WidgetStyle.NEON), AppearancePreferences.state.value)
        AppearancePreferences.setApp(context, AppStyle.STUDIO)
        assertEquals(WidgetStyle.NEON, AppearancePreferences.read(context).widget)
    }

    @Test fun everyThemeAndPageInflatesAndKeepsControlsVisible() {
        for (style in WidgetStyle.entries) for (page in 0..2) {
            for ((width, height) in listOf(110 to 180, 240 to 300, 250 to 340)) {
                val view = render(style, page, width, height)
                val rows = view.findViewById<LinearLayout>(R.id.widget_container)
                val title = view.findViewById<TextView>(R.id.widget_title)
                assertTrue(title.text.isNotBlank())
                val switch = view.findViewById<View>(R.id.widget_switch)
                assertTrue((switch as TextView).text.isNotBlank())
                val rect = Rect()
                switch.getDrawingRect(rect)
                (view as android.view.ViewGroup).offsetDescendantRectToMyCoords(switch, rect)
                assertTrue("$style/$page controls outside $height: $rect", rect.bottom <= height)
                for (i in 0 until rows.childCount) {
                    val row = rows.getChildAt(i)
                    assertTrue("Row must be actionable", row.hasOnClickListeners())
                    row.getDrawingRect(rect)
                    view.offsetDescendantRectToMyCoords(row, rect)
                    assertTrue("$style/$page row clips at $height", rect.bottom <= height)
                    if (style != WidgetStyle.CLASSIC) {
                        val listRect = Rect()
                        val listFrame = rows.parent as View
                        listFrame.getDrawingRect(listRect)
                        view.offsetDescendantRectToMyCoords(listFrame, listRect)
                        assertTrue("$style row overlaps controls", rect.bottom <= listRect.bottom)
                    }
                }
                assertEquals(if (page == 1) View.GONE else View.VISIBLE,
                    view.findViewById<View>(R.id.widget_add).visibility)
                if (width == 240 && page == 0) save(view, "widget-${style.name.lowercase()}", width, height)
            }
        }
    }

    @Test fun overflowAndEmptyStatesDescribeTheActualContent() {
        val small = render(WidgetStyle.PAPER, 0, 110, 180)
        assertTrue(small.findViewById<TextView>(R.id.widget_overflow).text.contains("more"))
        val empty = TaskWidget.createViews(context, 7, 0, emptyList(), emptyList(), emptyList(), WidgetStyle.NEON, 300)
            .apply(context, FrameLayout(context))
        assertEquals(View.VISIBLE, empty.findViewById<View>(R.id.widget_empty).visibility)
        assertEquals("", empty.findViewById<TextView>(R.id.widget_overflow).text.toString())
    }

    @Test fun largeFontWidgetsStillKeepControlsInsideTheirBounds() {
        RuntimeEnvironment.setFontScale(1.6f)
        try {
            for (style in WidgetStyle.entries.filter { it != WidgetStyle.CLASSIC }) {
                val view = render(style, 0, 180, 220) as android.view.ViewGroup
                val rect = Rect()
                val controls = view.findViewById<View>(R.id.widget_switch)
                controls.getDrawingRect(rect)
                view.offsetDescendantRectToMyCoords(controls, rect)
                assertTrue("Large text controls clipped for $style", rect.bottom <= 220)
            }
        } finally {
            RuntimeEnvironment.setFontScale(1f)
        }
    }

    private fun render(style: WidgetStyle, page: Int, width: Int, height: Int): View {
        val views = TaskWidget.createViews(context, 7, page, tasks, emptyList(),
            listOf(Note(id = 1, content = "Small steps count", pinned = true)), style, height)
        return views.apply(context, FrameLayout(context)).apply {
            measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            layout(0, 0, width, height)
            viewTreeObserver.dispatchOnPreDraw()
        }
    }

    private fun save(view: View, name: String, width: Int, height: Int) {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        controller.get().setContentView(view)
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
        view.viewTreeObserver.dispatchOnPreDraw()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val file = File("build/appearance-previews/$name.png")
        requireNotNull(file.parentFile).mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        controller.pause().stop().destroy()
    }
}
