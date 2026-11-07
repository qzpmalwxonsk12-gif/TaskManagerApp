package com.skai.taskmanager.widget

import com.skai.taskmanager.R
import com.skai.taskmanager.ui.theme.WidgetStyle

data class WidgetPalette(
    val background: Int, val ink: Int, val muted: Int, val accent: Int,
    val button: Int, val row: Int, val caption: String
)

fun WidgetStyle.palette() = when (this) {
    WidgetStyle.PAPER -> WidgetPalette(R.drawable.widget_paper_art, 0xFF493C32.toInt(), 0xFF766553.toInt(),
        0xFF806345.toInt(), R.drawable.widget_paper_button, R.drawable.widget_paper_row, "THE DAILY EDIT")
    WidgetStyle.NEON -> WidgetPalette(R.drawable.widget_neon_art, 0xFFEAFBFF.toInt(), 0xFFA6C2D5.toInt(),
        0xFF71F5EE.toInt(), R.drawable.widget_neon_button, R.drawable.widget_neon_row, "FOCUS SYSTEM / ONLINE")
    WidgetStyle.BOTANICAL -> WidgetPalette(R.drawable.widget_botanical_art, 0xFFF4EED8.toInt(), 0xFFB9CDB9.toInt(),
        0xFFE5C889.toInt(), R.drawable.widget_botanical_button, R.drawable.widget_botanical_row, "GROW AT YOUR OWN PACE")
    WidgetStyle.CLASSIC -> WidgetPalette(R.drawable.widget_background, 0xFFF2F2F2.toInt(), 0xFFBBBBBB.toInt(),
        0xFFF2F2F2.toInt(), R.drawable.widget_button_bg, R.drawable.widget_button_bg, "TASKS")
}

/** Reserve space for controls and scale the row budget with accessibility font size. */
fun widgetRowBudget(heightDp: Int, fontScale: Float, style: WidgetStyle): Int {
    val classic = style == WidgetStyle.CLASSIC
    val scale = fontScale.coerceAtLeast(1f)
    val artSpace = if (style == WidgetStyle.PAPER && heightDp >= 260) 36 else 0
    val overhead = if (classic) 90f else (if (heightDp < 220) 122f else 140f) * scale + artSpace
    val rowHeight = if (classic) 30f * scale else 48f * scale
    return ((heightDp - overhead) / rowHeight).toInt().coerceIn(0, 14)
}
