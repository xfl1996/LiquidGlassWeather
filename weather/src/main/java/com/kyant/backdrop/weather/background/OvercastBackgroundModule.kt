package com.kyant.backdrop.weather.background

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 水滴数据
 */
private data class DropData(
    val id: Int,
    val startX: Float,
    val startY: Float,
    val width: Float,
    val height: Float,
    val delayMs: Int,
    val speedFactor: Float
)

private fun generateDrops(): List<DropData> {
    val rng = Random(42)
    return listOf(
        DropData(0, 0.35f, 0.15f, 3.5f, 5f, 0, 1f),
        DropData(1, 0.52f, 0.08f, 2.5f, 4f, 800, 0.9f),
        DropData(2, 0.68f, 0.20f, 3f, 5.5f, 1600, 1.1f),
        DropData(3, 0.82f, 0.10f, 2.8f, 4.5f, 2400, 0.95f)
    )
}

/**
 * 阴天背景模块
 */
class OvercastBackgroundModule : WeatherBackground {

    override fun getType(): WeatherBackgroundType = WeatherBackgroundType.OVERCAST
    override fun getName(): String = "阴天背景"

    override fun supports(weatherCode: Int, cloudTotal: Int): Boolean {
        return weatherCode in listOf(101, 102, 103, 104) || cloudTotal > 80
    }

    @Composable
    override fun BackgroundLayer(
        modifier: Modifier,
        params: WeatherBackgroundParams
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "overcast_bg")

        val scale by infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(params.animationDuration, easing = LinearEasing), RepeatMode.Reverse),
            label = "bgScale"
        )
        val translateX by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = -0.01f,
            animationSpec = infiniteRepeatable(tween(params.animationDuration, easing = LinearEasing), RepeatMode.Reverse),
            label = "bgTranslateX"
        )
        val translateY by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 0.01f,
            animationSpec = infiniteRepeatable(tween(params.animationDuration, easing = LinearEasing), RepeatMode.Reverse),
            label = "bgTranslateY"
        )

        val gradientColors = listOf(params.darkColor, params.primaryColor, params.lightColor)
        val angleRad = Math.toRadians(160.0)
        val startX = (0.5f - 0.5f * cos(angleRad)).toFloat()
        val startY = (0.5f - 0.5f * sin(angleRad)).toFloat()
        val endX = (0.5f + 0.5f * cos(angleRad)).toFloat()
        val endY = (0.5f + 0.5f * sin(angleRad)).toFloat()

        Canvas(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = size.width * translateX
                    translationY = size.height * translateY
                }
        ) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(size.width * startX, size.height * startY),
                    end = Offset(size.width * endX, size.height * endY)
                )
            )
        }
    }

    @Composable
    override fun GlowLayer(
        modifier: Modifier,
        params: GlowParams
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "overcast_glow")

        val coolX by infiniteTransition.animateFloat(
            initialValue = -0.05f, targetValue = 0.05f,
            animationSpec = infiniteRepeatable(tween(params.coolGlowDuration, easing = LinearEasing), RepeatMode.Reverse),
            label = "coolGlowX"
        )
        val coolY by infiniteTransition.animateFloat(
            initialValue = -0.1f, targetValue = 0.15f,
            animationSpec = infiniteRepeatable(tween(params.coolGlowDuration, easing = LinearEasing), RepeatMode.Reverse),
            label = "coolGlowY"
        )
        val warmX by infiniteTransition.animateFloat(
            initialValue = 0.1f, targetValue = -0.1f,
            animationSpec = infiniteRepeatable(tween(params.warmGlowDuration, easing = LinearEasing), RepeatMode.Reverse),
            label = "warmGlowX"
        )
        val warmY by infiniteTransition.animateFloat(
            initialValue = 0.05f, targetValue = -0.1f,
            animationSpec = infiniteRepeatable(tween(params.warmGlowDuration, easing = LinearEasing), RepeatMode.Reverse),
            label = "warmGlowY"
        )

        Box(modifier = modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier.fillMaxSize()
                    .graphicsLayer { translationX = size.width * coolX; translationY = size.height * coolY }
            ) {
                val glowSize = params.coolGlowSize * density
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(params.coolGlowColor, Color.Transparent),
                        center = Offset(size.width / 2, size.height / 2),
                        radius = glowSize
                    ),
                    radius = glowSize,
                    center = Offset(size.width / 2, size.height / 2)
                )
            }
            Canvas(
                modifier = Modifier.fillMaxSize()
                    .graphicsLayer { translationX = size.width * warmX; translationY = size.height * warmY }
            ) {
                val glowSize = params.warmGlowSize * density
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(params.warmGlowColor, Color.Transparent),
                        center = Offset(size.width / 2, size.height / 2),
                        radius = glowSize
                    ),
                    radius = glowSize,
                    center = Offset(size.width / 2, size.height / 2)
                )
            }
        }
    }

    /**
     * 水滴层：4 个水滴 + 4 秒 drip 动画 + 局部透镜折射
     */
    @Composable
    fun DripLayer(
        modifier: Modifier = Modifier,
        params: WaterDropParams = WaterDropParams()
    ) {
        if (!params.enableAnimation) return

        val drops = remember { generateDrops() }
        val infiniteTransition = rememberInfiniteTransition(label = "drip")

        val dropAnimations = drops.map { drop ->
            val progress by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween((4000 * drop.speedFactor).toInt(), easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "dropY_${drop.id}"
            )
            Pair(drop, progress)
        }

        Canvas(modifier = modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            dropAnimations.forEach { (drop, progress) ->
                val currentY = (drop.startY + progress * 0.6f) * h
                val currentX = drop.startX * w

                val alpha = when {
                    progress < 0.2f -> progress / 0.2f
                    progress < 0.8f -> 1f
                    else -> (1f - progress) / 0.2f
                }.coerceIn(0f, 1f) * 0.35f

                val dropColor = params.dropColor.copy(alpha = alpha)
                val dropW = drop.width * density
                val dropH = drop.height * density

                // 水滴主体
                drawOval(
                    color = dropColor,
                    topLeft = Offset(currentX - dropW / 2, currentY - dropH / 2),
                    size = Size(dropW, dropH)
                )

                // 高光
                if (alpha > 0.1f) {
                    drawCircle(
                        color = Color.White.copy(alpha = alpha * 0.6f),
                        radius = dropW * 0.2f,
                        center = Offset(currentX - dropW * 0.15f, currentY - dropH * 0.2f)
                    )
                }

                // 局部透镜折射光圈
                if (alpha > 0.05f) {
                    val lensRadius = dropW * 1.8f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = alpha * 0.08f), Color.Transparent),
                            center = Offset(currentX, currentY),
                            radius = lensRadius
                        ),
                        radius = lensRadius,
                        center = Offset(currentX, currentY)
                    )
                }
            }
        }
    }
}
