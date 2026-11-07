package com.skai.taskmanager.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppStyle(val label: String) { STUDIO("Studio"), CLASSIC("Original") }

enum class WidgetStyle(val label: String, val description: String) {
    PAPER("Paper Garden", "Ivory stationery, flowers & sleepy little companions"),
    NEON("Neon Night", "Electric cyan, neon pink & midnight circuitry"),
    BOTANICAL("Botanical", "Forest green, golden details & quiet leaves"),
    CLASSIC("Classic", "The original compact charcoal widget")
}

data class Appearance(val app: AppStyle = AppStyle.STUDIO, val widget: WidgetStyle = WidgetStyle.PAPER)

/** Presentation preferences are separate from task data; no database migration is needed. */
object AppearancePreferences {
    private val mutable = MutableStateFlow(Appearance())
    val state = mutable.asStateFlow()

    fun initialize(context: Context) { mutable.value = read(context) }

    fun read(context: Context): Appearance {
        val prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
        return Appearance(
            AppStyle.entries.firstOrNull { it.name == prefs.getString("app_style", null) } ?: AppStyle.STUDIO,
            WidgetStyle.entries.firstOrNull { it.name == prefs.getString("widget_style", null) } ?: WidgetStyle.PAPER
        )
    }

    fun setApp(context: Context, style: AppStyle) {
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().putString("app_style", style.name).apply()
        mutable.value = mutable.value.copy(app = style)
    }

    fun setWidget(context: Context, style: WidgetStyle) {
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().putString("widget_style", style.name).apply()
        mutable.value = mutable.value.copy(widget = style)
    }
}
