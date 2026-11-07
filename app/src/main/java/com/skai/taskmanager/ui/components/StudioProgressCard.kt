package com.skai.taskmanager.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StudioProgressCard(completed: Int, total: Int, modifier: Modifier = Modifier) {
    val fraction = if (total == 0) 0f else completed.toFloat() / total
    val progress by animateFloatAsState(fraction, label = "studio progress")
    val cream = Color(0xFFF8F3DF)
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF284B3D), Color(0xFF456D56))))) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width * .97f, size.height * .14f)
            listOf(64.dp, 92.dp, 122.dp).forEach {
                drawCircle(cream.copy(alpha = .12f), it.toPx(), center, style = Stroke(1.dp.toPx()))
            }
            drawCircle(cream.copy(alpha = .08f), 46.dp.toPx(), Offset(size.width, size.height))
        }
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("A LITTLE PROGRESS, EVERY DAY", color = cream.copy(alpha = .78f),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (total > 0 && completed == total) "Beautifully done." else "Make room\nfor what matters.",
                        fontFamily = FontFamily.Serif, fontSize = 28.sp, lineHeight = 33.sp, color = cream)
                    Text(if (total == 0) "Your day starts with one small step." else "$completed of $total tasks complete",
                        Modifier.padding(top = 10.dp), color = cream.copy(alpha = .8f),
                        style = MaterialTheme.typography.bodyMedium)
                }
                Box(Modifier.padding(start = 12.dp).size(64.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(),
                        color = Color(0xFFE5C78B), trackColor = cream.copy(alpha = .15f), strokeWidth = 4.dp)
                    Text("${(fraction * 100).toInt()}%", color = cream, style = MaterialTheme.typography.titleMedium)
                }
            }
            HorizontalDivider(color = cream.copy(alpha = .15f))
            Text(if (total > completed) "${total - completed} things to look forward to" else "Take a breath. This moment is yours.",
                color = cream.copy(alpha = .85f), style = MaterialTheme.typography.labelMedium)
        }
    }
}
