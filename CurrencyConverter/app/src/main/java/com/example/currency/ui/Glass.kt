package com.example.currency.ui

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val Blue = Color(0xFF3B82F6)
val Purple = Color(0xFF8B5CF6)
val Slate = Color(0xFF94A3B8)
val ErrorRed = Color(0xFFEF4444)

/**
 * Liquid glass surface: soft shadow, translucent gradient tint and a gradient hairline border.
 * Compose has no true "backdrop blur" API, so the blur lives in [GlassBox] (API 31+);
 * below Android 12 the tint is slightly stronger instead (fallback, no blur).
 */
fun Modifier.liquidGlass(
    blurRadius: Dp = 20.dp,
    cornerRadius: Dp = 28.dp,
    tintAlpha: Float = 0.08f,
    borderAlpha: Float = 0.3f,
    borderBrush: Brush? = null
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val fallbackBoost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0f else 0.06f
    return this
        .shadow(8.dp, shape, ambientColor = Blue, spotColor = Purple)
        .clip(shape)
        .background(Brush.linearGradient(listOf(Color.White.copy(tintAlpha + fallbackBoost), Color.Transparent)))
        .border(1.dp, borderBrush ?: Brush.linearGradient(listOf(Color.White.copy(borderAlpha), Color.Transparent)), shape)
}

/** Glass container: a blurred glow layer under the crisp content (blur only on Android 12+). */
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 20.dp,
    cornerRadius: Dp = 28.dp,
    tintAlpha: Float = 0.08f,
    borderAlpha: Float = 0.3f,
    borderBrush: Brush? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.liquidGlass(blurRadius, cornerRadius, tintAlpha, borderAlpha, borderBrush)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Box(Modifier.matchParentSize()
                .graphicsLayer { renderEffect = BlurEffect(blurRadius.toPx(), blurRadius.toPx()) }
                .background(Brush.radialGradient(listOf(Blue.copy(0.12f), Purple.copy(0.06f), Color.Transparent))))
        }
        content()
    }
}

/** Dark gradient with two slowly drifting radial light spots (20s loop). */
@Composable
fun AuroraBackground(dark: Boolean, content: @Composable () -> Unit) {
    val t by rememberInfiniteTransition(label = "aurora").animateFloat(
        0f, 1f, infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse), label = "t")
    val colors = if (dark) listOf(Color(0xFF0A0E1A), Color(0xFF1A0E2A), Color.Black)
                 else listOf(Color(0xFFEFF4FF), Color(0xFFF3EEFF), Color.White)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colors)).drawBehind {
        val r = size.maxDimension * 0.6f
        val a = Offset(size.width * (0.1f + 0.3f * t), size.height * (0.1f + 0.2f * t))
        val b = Offset(size.width * (0.9f - 0.3f * t), size.height * (0.9f - 0.2f * t))
        drawCircle(Brush.radialGradient(listOf(Blue.copy(0.35f), Color.Transparent), a, r), r, a)
        drawCircle(Brush.radialGradient(listOf(Purple.copy(0.35f), Color.Transparent), b, r), r, b)
    }) { content() }
}

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    val scheme = if (dark) darkColorScheme(primary = Blue, secondary = Purple, onBackground = Color.White,
            onSurface = Color.White, onSurfaceVariant = Slate, error = ErrorRed, surface = Color(0xFF14101F))
        else lightColorScheme(primary = Blue, secondary = Purple, onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A), onSurfaceVariant = Color(0xFF475569), error = ErrorRed)
    MaterialTheme(colorScheme = scheme, content = content)
}
