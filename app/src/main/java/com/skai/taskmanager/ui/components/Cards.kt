package com.skai.taskmanager.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Shared corner shape for all list cards, so the app has one consistent card look. */
val AppCardShape: RoundedCornerShape
    @Composable get() = RoundedCornerShape(if (com.skai.taskmanager.ui.theme.LocalStudioStyle.current) 24.dp else 16.dp)

/** Shared container color for list cards (tasks, notes, sessions). */
@Composable
fun appCardColors(): CardColors = CardDefaults.cardColors(
    containerColor = if (com.skai.taskmanager.ui.theme.LocalStudioStyle.current)
        MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
)

@Composable
fun appCardBorder(): BorderStroke? = if (com.skai.taskmanager.ui.theme.LocalStudioStyle.current)
    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)) else null
