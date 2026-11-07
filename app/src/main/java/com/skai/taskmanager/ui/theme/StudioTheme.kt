package com.skai.taskmanager.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

val LocalStudioStyle = staticCompositionLocalOf { false }

val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val StudioLight = lightColorScheme(
    primary = Color(0xFF365E51), onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEBDD), onPrimaryContainer = Color(0xFF193C30),
    secondary = Color(0xFF856344), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2E3CE), onSecondaryContainer = Color(0xFF493821),
    tertiary = Color(0xFF9C5E68), tertiaryContainer = Color(0xFFF5DFE3),
    background = Color(0xFFF7F5EF), onBackground = Color(0xFF263B32),
    surface = Color(0xFFFFFDF8), onSurface = Color(0xFF263B32),
    surfaceVariant = Color(0xFFECEEE4), onSurfaceVariant = Color(0xFF626D63),
    outline = Color(0xFF7A877C), outlineVariant = Color(0xFFDCE1D5)
)

val StudioDark = darkColorScheme(
    primary = Color(0xFFB1D6BE), onPrimary = Color(0xFF173B2B),
    primaryContainer = Color(0xFF294D3C), onPrimaryContainer = Color(0xFFD9EFDF),
    secondary = Color(0xFFE5C59E), onSecondary = Color(0xFF41301B),
    secondaryContainer = Color(0xFF4E402E), onSecondaryContainer = Color(0xFFF4E1C8),
    tertiary = Color(0xFFE8B7C1), tertiaryContainer = Color(0xFF573640),
    background = Color(0xFF131D19), onBackground = Color(0xFFE1E9DF),
    surface = Color(0xFF1E2A23), onSurface = Color(0xFFE1E9DF),
    surfaceVariant = Color(0xFF2C382F), onSurfaceVariant = Color(0xFFB6C4B7),
    outline = Color(0xFF899B8C), outlineVariant = Color(0xFF3A493E)
)

val StudioTypography = Typography.copy(
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 36.sp, lineHeight = 42.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 30.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 25.sp, lineHeight = 32.sp),
    titleMedium = Typography.titleMedium.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
    bodyLarge = Typography.bodyLarge.copy(letterSpacing = 0.sp),
    bodyMedium = Typography.bodyMedium.copy(letterSpacing = 0.sp)
)
