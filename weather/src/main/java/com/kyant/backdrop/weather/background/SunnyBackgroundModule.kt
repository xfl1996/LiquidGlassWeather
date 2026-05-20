package com.kyant.backdrop.weather.background

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 晴天背景模块（预留）
 * 后期实现：阳光、蓝天、云彩效果
 */
class SunnyBackgroundModule : WeatherBackground {

    override fun getType(): WeatherBackgroundType = WeatherBackgroundType.SUNNY

    override fun getName(): String = "晴天背景"

    override fun supports(weatherCode: Int, cloudTotal: Int): Boolean {
        // 和风天气晴天代码: 100(晴), 150(晴转多云)
        return weatherCode in listOf(100, 150) && cloudTotal < 30
    }

    @Composable
    override fun BackgroundLayer(
        modifier: Modifier,
        params: WeatherBackgroundParams
    ) {
        // 占位效果：简单的蓝色渐变
        Canvas(modifier = modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1a3a5c),  // 深蓝
                        Color(0xFF4a90d9),  // 天蓝
                        Color(0xFF87CEEB)   // 浅蓝
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
            )
        }
    }

    @Composable
    override fun GlowLayer(
        modifier: Modifier,
        params: GlowParams
    ) {
        // 占位效果：简单的阳光光晕
        Box(modifier = modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x33FFD700),  // 金色阳光
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.7f, size.height * 0.3f),
                        radius = size.width * 0.4f
                    ),
                    radius = size.width * 0.4f,
                    center = Offset(size.width * 0.7f, size.height * 0.3f)
                )
            }
        }
    }
}
