package com.kyant.backdrop.weather

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

/**
 * Nixie tube (辉光管) text effect.
 * Dark tube background + amber glowing digits + subtle flicker.
 */
@Composable
fun NixieText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 72,
    fontFamily: FontFamily = FontFamily.Monospace,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val textMeasurer = rememberTextMeasurer()

    val infiniteTransition = rememberInfiniteTransition(label = "nixie")
    val flicker by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )

    // Subtle shimmer for individual digits
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    val style = TextStyle(
        fontSize = fontSize.sp,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        color = Color(0xFFFF6F00)
    )

    val textLayout = remember(text, fontSize) {
        textMeasurer.measure(text, style)
    }

    val digitCount = remember(text) { text.length }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .drawBehind {
                // Dark tube background
                drawRoundRect(
                    color = Color(0xFF0A0A0A),
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                )
                // Inner glass sheen
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x08FFFFFF),
                            Color.Transparent,
                            Color(0x03FFFFFF)
                        )
                    ),
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                )

                val digitW = textLayout.size.width.toFloat() / digitCount.coerceAtLeast(1)
                val textX = (size.width - textLayout.size.width) / 2f
                val textY = (size.height - textLayout.size.height) / 2f

                // Draw each digit with individual glow
                var xOffset = textX
                val measureStyle = TextStyle(
                    fontSize = fontSize.sp,
                    fontFamily = fontFamily,
                    fontWeight = fontWeight
                )
                for (ch in text) {
                    val charStr = ch.toString()
                    val charLayout = textMeasurer.measure(charStr, measureStyle)
                    val cx = xOffset + digitW / 2f

                    // Base warm glow
                    drawText(
                        charLayout,
                        topLeft = Offset(xOffset, textY),
                        color = Color(0xFFCC5500).copy(alpha = 0.4f * flicker)
                    )

                    // Wide ambient glow
                    drawText(
                        charLayout,
                        topLeft = Offset(xOffset - 1.dp.toPx(), textY - 1.dp.toPx()),
                        color = Color(0xFFFF8C00).copy(alpha = 0.15f * flicker),
                        blendMode = BlendMode.Screen
                    )

                    // Tight bright halo
                    drawText(
                        charLayout,
                        topLeft = Offset(xOffset + 0.5.dp.toPx(), textY + 0.5.dp.toPx()),
                        color = Color(0xFFFFAA33).copy(alpha = 0.2f * flicker),
                        blendMode = BlendMode.Screen
                    )

                    // Per-digit shimmer variation
                    val digitIndex = text.indexOf(ch).coerceAtLeast(0)
                    val digitFlicker = 0.85f + 0.15f * sin(digitIndex * 2.1 + shimmer * 6.28f).toFloat()

                    // Main bright digit
                    drawText(
                        charLayout,
                        topLeft = Offset(xOffset, textY),
                        color = Color(0xFFFFCC66).copy(alpha = digitFlicker * flicker)
                    )

                    // Hot center highlight
                    drawText(
                        charLayout,
                        topLeft = Offset(xOffset - 0.3.dp.toPx(), textY - 0.3.dp.toPx()),
                        color = Color(0xFFFFEEBB).copy(alpha = 0.25f * flicker)
                    )

                    xOffset += digitW
                }
            }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        // Invisible text for sizing
        Box(modifier = Modifier.height(0.dp))
    }
}

/**
 * Compact nixie text for smaller displays (e.g., AQI value).
 */
@Composable
fun NixieTextSmall(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 28
) {
    NixieText(
        text = text,
        modifier = modifier,
        fontSize = fontSize,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
    )
}
