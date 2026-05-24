package com.kyant.backdrop.weather

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    backgroundResId: Int
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val backdrop = rememberLayerBackdrop()
    val settingsManager = remember { CardSettingsManager(context) }
    var settings by remember { mutableStateOf(settingsManager.load()) }

    fun save() { settingsManager.save(settings) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(backgroundResId),
            contentDescription = null,
            modifier = Modifier.layerBackdrop(backdrop).fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ═══ Header ═══
            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassButton("← 返回", onClick = onBack)
                    Spacer(Modifier.weight(1f))
                    BasicText(
                        "设置",
                        style = appTextStyle(fontSize = 24.sp, fontWeight = FontWeight.Medium, color = White)
                    )
                    Spacer(Modifier.weight(1f))
                    // placeholder for balance
                    Spacer(Modifier.width(80.dp))
                }
            }

            // ═══ Global Glass Params ═══
            item(key = "global_params") {
                SectionTitle("全局玻璃参数", backdrop)
                Spacer(Modifier.height(8.dp))
                GlassParamsPanel(
                    params = settings.globalParams,
                    onParamsChange = {
                        settings = settings.copy(globalParams = it)
                        save()
                    },
                    backdrop = backdrop
                )
                Spacer(Modifier.height(8.dp))
            }

            // ═══ Per-Weather Font Color ═══
            item(key = "font_color") {
                SectionTitle("字体颜色", backdrop)
                Spacer(Modifier.height(8.dp))
            }
            WEATHER_CONDITION_DISPLAY_NAMES.forEach { (condition, displayName) ->
                val defaultHex = WEATHER_TEXT_COLOR_DEFAULTS[condition] ?: "#FFFFFF"
                val currentHex = settings.textColorOverrides[condition.name] ?: ""

                item(key = "color_${condition.name}") {
                    var hexInput by remember(settings.textColorOverrides, condition.name) {
                        mutableStateOf(currentHex)
                    }
                    var isValid by remember { mutableStateOf(true) }

                    GlassCard(backdrop, settings.globalParams, Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                            // Row: color dot + name + hex input + apply
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Color preview
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(
                                            try { Color(android.graphics.Color.parseColor(if (currentHex.isEmpty()) defaultHex else currentHex)) }
                                            catch (e: Exception) { White }
                                        )
                                )
                                Spacer(Modifier.width(8.dp))
                                // Name
                                BasicText(
                                    displayName,
                                    style = appTextStyle(fontSize = 13.sp, color = White),
                                    modifier = Modifier.weight(1f)
                                )
                                // Hex input
                                Box(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = hexInput,
                                        onValueChange = { newHex ->
                                            hexInput = newHex
                                            val cleanHex = if (newHex.startsWith("#")) newHex else "#$newHex"
                                            isValid = try {
                                                android.graphics.Color.parseColor(cleanHex)
                                                true
                                            } catch (e: Exception) { false }
                                        },
                                        textStyle = appTextStyle(
                                            fontSize = 12.sp,
                                            color = White
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        decorationBox = { inner ->
                                            Box {
                                                if (hexInput.isEmpty()) {
                                                    BasicText(defaultHex, style = appTextStyle(fontSize = 14.sp, color = White.copy(alpha = 0.3f)))
                                                }
                                                inner()
                                            }
                                        }
                                    )
                                }
                                // Apply
                                if (isValid && hexInput.isNotEmpty() && hexInput != currentHex) {
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                val hex = if (hexInput.startsWith("#")) hexInput else "#$hexInput"
                                                val newMap = settings.textColorOverrides.toMutableMap()
                                                newMap[condition.name] = hex
                                                settings = settings.copy(textColorOverrides = newMap)
                                                save()
                                            }
                                            .background(Color.White.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BasicText("✓", style = appTextStyle(fontSize = 14.sp, color = White))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // ═══ Card Order & Visibility & Per-Card Params ═══
            item(key = "card_list_title") {
                SectionTitle("卡片管理", backdrop)
                Spacer(Modifier.height(8.dp))
            }

            settings.order.forEachIndexed { index, cardId ->
                val isHidden = cardId in settings.hidden
                val title = CARD_TITLES[cardId] ?: cardId
                val perCardParams = settings.cardParams[cardId]

                item(key = "card_$cardId") {
                    var expanded by remember { mutableStateOf(false) }

                    GlassCard(backdrop, settings.globalParams, Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                        // ── Row 1: order controls + title + visibility ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ▲ Move up
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (index > 0) Modifier.pointerInput(Unit) {
                                            detectTapGestures {
                                                val newOrder = settings.order.toMutableList()
                                                val item = newOrder.removeAt(index)
                                                newOrder.add(index - 1, item)
                                                settings = settings.copy(order = newOrder)
                                                save()
                                            }
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText("▲", style = appTextStyle(
                                    fontSize = 12.sp,
                                    color = if (index > 0) White else White.copy(alpha = 0.3f)
                                ))
                            }

                            Spacer(Modifier.width(4.dp))

                            // ▼ Move down
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (index < settings.order.size - 1) Modifier.pointerInput(Unit) {
                                            detectTapGestures {
                                                val newOrder = settings.order.toMutableList()
                                                val item = newOrder.removeAt(index)
                                                newOrder.add(index + 1, item)
                                                settings = settings.copy(order = newOrder)
                                                save()
                                            }
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText("▼", style = appTextStyle(
                                    fontSize = 12.sp,
                                    color = if (index < settings.order.size - 1) White else White.copy(alpha = 0.3f)
                                ))
                            }

                            // Title
                            BasicText(
                                title,
                                style = appTextStyle(
                                    fontSize = 14.sp,
                                    color = if (isHidden) White.copy(alpha = 0.3f) else White,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            )

                            // Toggle visibility
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .pointerInput(Unit) {
                                        detectTapGestures {
                                            val newHidden = settings.hidden.toMutableSet()
                                            if (isHidden) newHidden.remove(cardId) else newHidden.add(cardId)
                                            settings = settings.copy(hidden = newHidden)
                                            save()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isHidden) {
                                    BasicText("✕", style = appTextStyle(fontSize = 14.sp, color = Color(0xFFEF4444)))
                                } else {
                                    BasicText("✓", style = appTextStyle(fontSize = 14.sp, color = Color(0xFF4ADE80)))
                                }
                            }

                            // Expand arrow
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .pointerInput(Unit) {
                                        detectTapGestures { expanded = !expanded }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText(
                                    if (expanded) "▼" else "▶",
                                    style = appTextStyle(fontSize = 11.sp, color = White.copy(alpha = 0.5f))
                                )
                            }
                        }

                        // ── Row 2: per-card params (expandable) ──
                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                // Divider line
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(White.copy(alpha = 0.15f))
                                )
                                Spacer(Modifier.height(8.dp))

                                // "Use global" toggle
                                val isUsingGlobal = perCardParams == null
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            detectTapGestures {
                                                if (isUsingGlobal) {
                                                    // Enable per-card: copy global as starting point
                                                    val newCardParams = settings.cardParams.toMutableMap()
                                                    newCardParams[cardId] = settings.globalParams
                                                    settings = settings.copy(cardParams = newCardParams)
                                                } else {
                                                    // Disable per-card: remove override
                                                    val newCardParams = settings.cardParams.toMutableMap()
                                                    newCardParams.remove(cardId)
                                                    settings = settings.copy(cardParams = newCardParams)
                                                }
                                                save()
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isUsingGlobal) Color(0xFF3B82F6) else Color(0xFF333333))
                                            .pointerInput(Unit) {
                                                detectTapGestures {
                                                    if (isUsingGlobal) {
                                                        val newCardParams = settings.cardParams.toMutableMap()
                                                        newCardParams[cardId] = settings.globalParams
                                                        settings = settings.copy(cardParams = newCardParams)
                                                    } else {
                                                        val newCardParams = settings.cardParams.toMutableMap()
                                                        newCardParams.remove(cardId)
                                                        settings = settings.copy(cardParams = newCardParams)
                                                    }
                                                    save()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!isUsingGlobal) {
                                            BasicText("✓", style = appTextStyle(fontSize = 11.sp, color = White))
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    BasicText(
                                        "使用全局参数",
                                        style = appTextStyle(fontSize = 12.sp, color = White.copy(alpha = 0.7f))
                                    )
                                }

                                // Per-card params sliders (only when NOT using global)
                                if (!isUsingGlobal) {
                                    Spacer(Modifier.height(8.dp))
                                    GlassParamsPanel(
                                        params = perCardParams!!,
                                        onParamsChange = { newParams ->
                                            val newCardParams = settings.cardParams.toMutableMap()
                                            newCardParams[cardId] = newParams
                                            settings = settings.copy(cardParams = newCardParams)
                                            save()
                                        },
                                        backdrop = backdrop
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom spacer
            item(key = "bottom_spacer") {
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════
// Reusable: Section Title inside glass backdrop
// ═══════════════════════════════════════════

@Composable
private fun SectionTitle(text: String, backdrop: LayerBackdrop) {
    GlassCard(backdrop, GlassParams(blur = 0f, corner = 0f), Modifier.padding(horizontal = 24.dp)) {
        BasicText(
            text,
            style = appTextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = White.copy(alpha = 0.8f)
            )
        )
    }
}

// ═══════════════════════════════════════════
// Reusable: Glass Params Panel (sliders)
// ═══════════════════════════════════════════

@Composable
private fun GlassParamsPanel(
    params: GlassParams,
    onParamsChange: (GlassParams) -> Unit,
    backdrop: LayerBackdrop
) {
    GlassCard(backdrop, params, Modifier.padding(horizontal = 24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            ParamSlider("模糊", params.blur, 0f, 30f) { onParamsChange(params.copy(blur = it)) }
            ParamSlider("折射高度", params.refH, 0f, 64f) { onParamsChange(params.copy(refH = it)) }
            ParamSlider("折射强度", params.refA, 0f, 128f) { onParamsChange(params.copy(refA = it)) }
            ParamSlider("圆角", params.corner, 0f, 64f) { onParamsChange(params.copy(corner = it)) }
            ParamSlider("颜色深度", params.cardAlpha, 0f, 100f) { onParamsChange(params.copy(cardAlpha = it)) }
        }
    }
}

// ═══════════════════════════════════════════
// Param Slider (tap + drag)
// ═══════════════════════════════════════════

@Composable
private fun ParamSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            label,
            style = appTextStyle(fontSize = 11.sp, color = White.copy(alpha = 0.6f)),
            modifier = Modifier.width(56.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(White.copy(alpha = 0.2f))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newValue = min + (offset.x / size.width) * (max - min)
                        onValueChange(newValue.coerceIn(min, max))
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange(min + fraction * (max - min))
                    }
                }
        ) {
            // Thumb
            val fraction = ((value - min) / (max - min)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = (fraction * 180).dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(White)
                )
            }
        }
        BasicText(
            "${value.roundToInt()}",
            style = appTextStyle(fontSize = 11.sp, color = White.copy(alpha = 0.5f)),
            modifier = Modifier.width(32.dp)
        )
    }
}