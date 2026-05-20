package com.kyant.backdrop.weather

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Simple weather icon drawn with Canvas - no external resources needed.
 */
@Composable
fun WeatherIconCanvas(
    icon: WeatherCondition,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f

        when (icon) {
            WeatherCondition.SUNNY -> {
                drawCircle(color = Color(0xFFFFD54F), radius = s * 0.22f, center = Offset(cx, cy))
                for (i in 0 until 8) {
                    val angle = Math.toRadians((i * 45.0))
                    val innerR = s * 0.3f
                    val outerR = s * 0.42f
                    drawLine(
                        color = Color(0xFFFFD54F),
                        start = Offset(cx + innerR * kotlin.math.cos(angle).toFloat(), cy + innerR * kotlin.math.sin(angle).toFloat()),
                        end = Offset(cx + outerR * kotlin.math.cos(angle).toFloat(), cy + outerR * kotlin.math.sin(angle).toFloat()),
                        strokeWidth = s * 0.05f, cap = StrokeCap.Round
                    )
                }
            }
            WeatherCondition.CLOUDY -> {
                drawCircle(color = tint.copy(alpha = 0.9f), radius = s * 0.2f, center = Offset(cx - s * 0.12f, cy + s * 0.05f))
                drawCircle(color = tint.copy(alpha = 0.9f), radius = s * 0.25f, center = Offset(cx + s * 0.08f, cy))
                drawCircle(color = tint.copy(alpha = 0.9f), radius = s * 0.18f, center = Offset(cx + s * 0.25f, cy + s * 0.05f))
            }
            WeatherCondition.PARTLY_CLOUDY -> {
                drawCircle(color = Color(0xFFFFD54F), radius = s * 0.18f, center = Offset(cx - s * 0.05f, cy - s * 0.1f))
                drawCircle(color = tint.copy(alpha = 0.9f), radius = s * 0.16f, center = Offset(cx - s * 0.08f, cy + s * 0.08f))
                drawCircle(color = tint.copy(alpha = 0.9f), radius = s * 0.2f, center = Offset(cx + s * 0.1f, cy + s * 0.03f))
            }
            WeatherCondition.RAINY -> {
                drawCircle(color = tint.copy(alpha = 0.7f), radius = s * 0.18f, center = Offset(cx - s * 0.1f, cy - s * 0.05f))
                drawCircle(color = tint.copy(alpha = 0.7f), radius = s * 0.22f, center = Offset(cx + s * 0.08f, cy - s * 0.08f))
                for (i in 0 until 3) {
                    val dropX = cx - s * 0.15f + i * s * 0.15f
                    drawLine(
                        color = Color(0xFF4FC3F7),
                        start = Offset(dropX, cy + s * 0.15f),
                        end = Offset(dropX - s * 0.04f, cy + s * 0.27f),
                        strokeWidth = s * 0.04f, cap = StrokeCap.Round
                    )
                }
            }
            WeatherCondition.THUNDERSTORM -> {
                drawCircle(color = tint.copy(alpha = 0.6f), radius = s * 0.2f, center = Offset(cx - s * 0.08f, cy - s * 0.1f))
                drawCircle(color = tint.copy(alpha = 0.6f), radius = s * 0.24f, center = Offset(cx + s * 0.1f, cy - s * 0.12f))
                drawLine(color = Color(0xFFFFEB3B), start = Offset(cx, cy + s * 0.02f), end = Offset(cx - s * 0.08f, cy + s * 0.2f), strokeWidth = s * 0.06f, cap = StrokeCap.Round)
                drawLine(color = Color(0xFFFFEB3B), start = Offset(cx - s * 0.08f, cy + s * 0.2f), end = Offset(cx + s * 0.05f, cy + s * 0.2f), strokeWidth = s * 0.06f, cap = StrokeCap.Round)
            }
            WeatherCondition.RAIN_THUNDER -> {
                drawCircle(color = tint.copy(alpha = 0.6f), radius = s * 0.18f, center = Offset(cx - s * 0.08f, cy - s * 0.12f))
                drawCircle(color = tint.copy(alpha = 0.6f), radius = s * 0.22f, center = Offset(cx + s * 0.1f, cy - s * 0.14f))
                drawLine(color = Color(0xFFFFEB3B), start = Offset(cx - s * 0.02f, cy), end = Offset(cx - s * 0.1f, cy + s * 0.15f), strokeWidth = s * 0.05f, cap = StrokeCap.Round)
                drawLine(color = Color(0xFFFFEB3B), start = Offset(cx - s * 0.1f, cy + s * 0.15f), end = Offset(cx + s * 0.03f, cy + s * 0.15f), strokeWidth = s * 0.05f, cap = StrokeCap.Round)
                for (i in 0 until 2) {
                    val dropX = cx + s * 0.1f + i * s * 0.12f
                    drawLine(
                        color = Color(0xFF4FC3F7),
                        start = Offset(dropX, cy + s * 0.1f),
                        end = Offset(dropX - s * 0.03f, cy + s * 0.2f),
                        strokeWidth = s * 0.035f, cap = StrokeCap.Round
                    )
                }
            }
            WeatherCondition.FOGGY -> {
                for (i in 0 until 4) {
                    val lineY = cy - s * 0.15f + i * s * 0.1f
                    drawLine(
                        color = tint.copy(alpha = 0.5f),
                        start = Offset(cx - s * 0.3f, lineY),
                        end = Offset(cx + s * 0.3f, lineY),
                        strokeWidth = s * 0.05f, cap = StrokeCap.Round
                    )
                }
            }
            WeatherCondition.SNOWY -> {
                drawCircle(color = tint.copy(alpha = 0.7f), radius = s * 0.2f, center = Offset(cx - s * 0.08f, cy - s * 0.1f))
                drawCircle(color = tint.copy(alpha = 0.7f), radius = s * 0.24f, center = Offset(cx + s * 0.1f, cy - s * 0.12f))
                for (i in 0 until 4) {
                    val dropX = cx - s * 0.15f + i * s * 0.1f
                    drawCircle(
                        color = Color(0xFFE1F5FE),
                        radius = s * 0.03f,
                        center = Offset(dropX, cy + s * 0.15f + (i % 2) * s * 0.05f)
                    )
                }
            }
        }
    }
}
