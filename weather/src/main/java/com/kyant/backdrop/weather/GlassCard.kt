package com.kyant.backdrop.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.RoundedRectangle
import kotlin.math.roundToInt

data class GlassParams(
    val blur: Float = 0f,
    val refH: Float = 17f,
    val refA: Float = 128f,
    val corner: Float = 43f,
    val cardAlpha: Float = 100f  // 0 = fully transparent, 100 = default glass effect
)

@Composable
fun GlassCard(
    backdrop: LayerBackdrop,
    params: GlassParams,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable () -> Unit
) {
    val cardAlpha = (params.cardAlpha / 100f).coerceIn(0f, 1f)

    Box(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedRectangle(params.corner.dp) },
                effects = {
                    vibrancy()
                    blur(params.blur.dp.toPx())
                    lens(
                        refractionHeight = params.refH.dp.toPx(),
                        refractionAmount = params.refA.dp.toPx()
                    )
                },
                highlight = { Highlight.Plain }
            )
            // Color depth overlay: darker = more opaque overlay
            .drawBehind {
                if (cardAlpha < 1f) {
                    // When alpha < 100, draw a lighter overlay to reduce the glass darkening effect
                    val overlayAlpha = 1f - cardAlpha
                    drawRoundRect(
                        color = Color.White.copy(alpha = overlayAlpha * 0.3f),
                        cornerRadius = CornerRadius(params.corner.dp.toPx()),
                        size = size
                    )
                }
            }
            .padding(24f.dp)
    ) {
        Column {
            if (title != null) {
                BasicText(
                    title,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = White.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            content()
        }
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(Color.White.copy(alpha = 0.2f))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text,
            style = TextStyle(
                fontSize = 14.sp,
                color = White
            )
        )
    }
}
