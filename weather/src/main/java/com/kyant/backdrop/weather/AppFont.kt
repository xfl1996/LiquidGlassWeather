package com.kyant.backdrop.weather

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * OPPO Sans 4.0 — 全局字体
 *
 * 通过 Android Typeface 从 assets 加载，避免 Compose Font() API 的 assetManager 要求。
 */
val LocalAppFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

@Composable
fun rememberAppFontFamily(): FontFamily {
    val context = LocalContext.current
    return remember {
        val typeface = Typeface.createFromAsset(context.assets, "fonts/oppo_sans.ttf")
        FontFamily(typeface)
    }
}

/**
 * 创建带 OPPO Sans 字体的 TextStyle
 */
@Composable
fun appTextStyle(
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified
): TextStyle {
    val fontFamily = LocalAppFontFamily.current
    return TextStyle(
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        fontSize = fontSize,
        color = color
    )
}
