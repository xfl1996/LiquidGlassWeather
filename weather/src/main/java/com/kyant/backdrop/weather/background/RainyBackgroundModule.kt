package com.kyant.backdrop.weather.background

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 雨天背景模块（预留）
 * 后期实现：雨滴、水面涟漪、灰暗天空效果
 */
class RainyBackgroundModule : WeatherBackground {

    override fun getType(): WeatherBackgroundType = WeatherBackgroundType.RAINY

    override fun getName(): String = "雨天背景"

    override fun supports(weatherCode: Int, cloudTotal: Int): Boolean {
        // 和风天气雨天代码: 300-312(阵雨), 313-399(雨夹雪/冻雨), 500-515(雪)
        return weatherCode in 300..399 || weatherCode in 500..515
    }

    @Composable
    override fun BackgroundLayer(
        modifier: Modifier,
        params: WeatherBackgroundParams
    ) {
        // 占位效果：灰暗渐变
        Canvas(modifier = modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2c3e50),  // 深灰
                        Color(0xFF4a6274),  // 灰蓝
                        Color(0xFF607d8b)   // 浅灰
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
        // 占位效果：无特殊光晕（雨天通常较暗）
        // 后期可添加雨滴效果
    }
}
