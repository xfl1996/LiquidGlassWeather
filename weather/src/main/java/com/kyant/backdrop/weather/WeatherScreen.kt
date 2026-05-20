package com.kyant.backdrop.weather

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

@Composable
fun WeatherScreen() {
    val context = LocalContext.current
    val backdrop = rememberLayerBackdrop()
    val settingsManager = remember { CardSettingsManager(context) }

    var currentPage by remember { mutableStateOf("weather") }
    var cardSettings by remember { mutableStateOf(settingsManager.load()) }
    val visibleCards = remember(cardSettings) {
        cardSettings.order.filter { it !in cardSettings.hidden }
    }

    // Resolve per-card params for a card
    fun resolveParams(cardId: String): GlassParams {
        return cardSettings.cardParams[cardId] ?: cardSettings.globalParams
    }

    // Handle system back button: settings → weather instead of exiting
    BackHandler(enabled = currentPage == "settings") {
        cardSettings = settingsManager.load()
        currentPage = "weather"
    }

    // Background res is shared between weather & settings pages
    var bgResId by androidx.compose.runtime.mutableIntStateOf(R.drawable.wallpaper_light)

    AnimatedContent(
        targetState = currentPage,
        transitionSpec = {
            if (targetState == "settings") {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 3 }
            } else {
                slideInHorizontally { -it / 3 } togetherWith slideOutHorizontally { it }
            }
        },
        label = "page_transition"
    ) { page ->
        when (page) {
            "settings" -> {
                SettingsScreen(
                    onBack = {
                        cardSettings = settingsManager.load()
                        currentPage = "weather"
                    },
                    backgroundResId = bgResId
                )
            }
            else -> {
                WeatherMainScreen(
                    backdrop = backdrop,
                    settings = cardSettings,
                    visibleCards = visibleCards,
                    onSettingsClick = { currentPage = "settings" },
                    onSettingsChange = { newSettings ->
                        cardSettings = newSettings
                        settingsManager.save(newSettings)
                    },
                    resolveParams = ::resolveParams,
                    onBackgroundChange = { bgResId = it }
                )
            }
        }
    }
}

@Composable
private fun WeatherMainScreen(
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    settings: CardSettingsManager.Settings,
    visibleCards: List<String>,
    onSettingsClick: () -> Unit,
    onSettingsChange: (CardSettingsManager.Settings) -> Unit,
    resolveParams: (String) -> GlassParams,
    onBackgroundChange: (Int) -> Unit
) {
    val context = LocalContext.current
    var weather by remember { mutableStateOf<WeatherResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // City search state
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<CityResult>>(emptyList()) }
    var isSearchingCity by remember { mutableStateOf(false) }

    // On start: load from cache only (no GPS). If no cache, show empty state.
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        val result = WeatherRepository.loadCachedOnly(context)
        if (result != null) {
            weather = result
            isLoading = false
        } else {
            // No cached data — show placeholder, don't locate
            isLoading = false
            weather = null
        }
    }

    fun refresh() {
        scope.launch {
            isLoading = true
            errorMessage = null
            val result = WeatherRepository.fetchAllWeather(context, forceRefresh = true)
            result.fold(
                onSuccess = { weather = it; isLoading = false },
                onFailure = { e -> errorMessage = e.message ?: "未知错误"; isLoading = false }
            )
        }
    }

    fun fetchCityWeather(city: CityResult) {
        scope.launch {
            isSearching = false
            searchQuery = ""
            searchResults = emptyList()
            isLoading = true
            isSearchingCity = true
            errorMessage = null
            val result = WeatherRepository.fetchWeatherForCity(context, city.name, city.latitude, city.longitude)
            result.fold(
                onSuccess = { weather = it; isLoading = false; isSearchingCity = false },
                onFailure = { e -> errorMessage = e.message ?: "未知错误"; isLoading = false; isSearchingCity = false }
            )
        }
    }

    fun backToAutoLocation() {
        scope.launch {
            isLoading = true
            errorMessage = null
            val result = WeatherRepository.fetchAllWeather(context, forceRefresh = true)
            result.fold(
                onSuccess = { weather = it; isLoading = false },
                onFailure = { e -> errorMessage = e.message ?: "未知错误"; isLoading = false }
            )
        }
    }

    // Dynamic background: default until weather data loads, then match weather type
    val bgResId = remember(weather) {
        WeatherBackgroundProvider.getBackgroundResourceOrNull(weather?.weatherIcon)
    }
    // Notify parent so settings page uses the same background
    LaunchedEffect(bgResId) {
        if (bgResId != null) onBackgroundChange(bgResId)
    }

    // Dynamic text color based on weather condition (or per-type override)
    val textColor = remember(weather, settings.textColorOverrides) {
        val condition = weather?.weatherIcon
        val overrideHex = condition?.let { settings.textColorOverrides[it.name] }
        overrideHex?.let { hex ->
            try {
                val clean = hex.removePrefix("#")
                if (clean.length == 6) {
                    Color(android.graphics.Color.parseColor("#$clean"))
                } else null
            } catch (_: Exception) { null }
        } ?: conditionToTextColor(condition)
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(bgResId),
            contentDescription = null,
            Modifier.layerBackdrop(backdrop).fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        when {
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GlassCard(backdrop, GlassParams(), Modifier.padding(horizontal = 32.dp)) {
                        BasicText("加载中...", style = TextStyle(fontSize = 18.sp, color = textColor))
                    }
                }
            }
            errorMessage != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GlassCard(backdrop, GlassParams(), Modifier.padding(horizontal = 32.dp)) {
                        BasicText(errorMessage ?: "未知错误", style = TextStyle(fontSize = 16.sp, color = textColor))
                        Spacer(Modifier.height(16.dp))
                        GlassButton("重试", onClick = { refresh() })
                    }
                }
            }
            weather != null -> {
                val data = weather!!
                val scope2 = rememberCoroutineScope()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ═══ Header ═══
                    item(key = "header") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.height(50.dp))

                            // Top row: city name + icon buttons (search, location, settings)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSearching) {
                                    // ═══ Search mode: text input + X to cancel ═══
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { query ->
                                                searchQuery = query
                                                if (query.length >= 2) {
                                                    scope.launch {
                                                        searchResults = WeatherService.searchCity(query)
                                                    }
                                                } else {
                                                    searchResults = emptyList()
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            textStyle = TextStyle(fontSize = 16.sp, color = textColor),
                                            singleLine = true,
                                            decorationBox = { inner ->
                                                Box {
                                                    if (searchQuery.isEmpty()) {
                                                        BasicText(
                                                            "搜索城市...",
                                                            style = TextStyle(fontSize = 16.sp, color = textColor.copy(alpha = 0.5f))
                                                        )
                                                    }
                                                    inner()
                                                }
                                            }
                                        )
                                        // X button to cancel
                                        BasicText(
                                            "✕",
                                            style = TextStyle(fontSize = 18.sp, color = textColor.copy(alpha = 0.7f)),
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .clickable {
                                                    isSearching = false
                                                    searchQuery = ""
                                                    searchResults = emptyList()
                                                }
                                        )
                                    }
                                } else {
                                    // ═══ Normal mode: city name + 3 icons ═══
                                    BasicText(
                                        data.cityName,
                                        style = TextStyle(
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = textColor
                                        )
                                    )
                                }

                                // Right: icon buttons (only when NOT searching)
                                if (!isSearching) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isSearchingCity) {
                                            BasicText("⏳", style = TextStyle(fontSize = 14.sp))
                                        }
                                        // Search
                                        Image(
                                            painter = painterResource(R.drawable.ic_search),
                                            contentDescription = "搜索城市",
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clickable { isSearching = true }
                                        )
                                        // Auto-location
                                        Image(
                                            painter = painterResource(R.drawable.ic_location),
                                            contentDescription = "回到定位",
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clickable {
                                                    backToAutoLocation()
                                                }
                                        )
                                        // Settings
                                        Image(
                                            painter = painterResource(R.drawable.ic_settings),
                                            contentDescription = "设置",
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clickable { onSettingsClick() }
                                        )
                                    }
                                }
                            }

                            // Search results dropdown
                            if (isSearching && searchResults.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                GlassCard(backdrop, GlassParams(), Modifier.padding(horizontal = 20.dp)) {
                                    searchResults.forEach { city ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { fetchCityWeather(city) }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(
                                                painter = painterResource(R.drawable.ic_location),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp).padding(end = 8.dp)
                                            )
                                            Column {
                                                BasicText(
                                                    city.name,
                                                    style = TextStyle(fontSize = 15.sp, color = textColor)
                                                )
                                                BasicText(
                                                    "${city.adminArea}, ${city.country}",
                                                    style = TextStyle(fontSize = 11.sp, color = textColor.copy(alpha = 0.5f))
                                                )
                                            }
                                        }
                                    }
                                }
                            } else if (isSearching && searchQuery.length >= 2) {
                                Spacer(Modifier.height(8.dp))
                                BasicText(
                                    "未找到匹配的城市",
                                    style = TextStyle(fontSize = 13.sp, color = textColor.copy(alpha = 0.5f))
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            BasicText(
                                "${data.currentTemp}°",
                                style = TextStyle(
                                    fontSize = 80.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            )

                            Spacer(Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WeatherIconCanvas(
                                    icon = data.weatherIcon,
                                    modifier = Modifier.size(32.dp),
                                    tint = textColor
                                )
                                Spacer(Modifier.width(8.dp))
                                BasicText(
                                    data.weatherDescription,
                                    style = TextStyle(fontSize = 18.sp, color = textColor)
                                )
                            }

                            BasicText(
                                "体感 ${data.feelsLike}°",
                                style = TextStyle(fontSize = 14.sp, color = textColor.copy(alpha = 0.7f))
                            )

                            Spacer(Modifier.height(24.dp))
                        }
                    }

                    // ═══ Weather Cards ═══
                    visibleCards.forEach { cardId ->
                        item(key = "card_$cardId") {
                            val params = resolveParams(cardId)
                            Spacer(Modifier.height(4.dp))
                            when (cardId) {
                                "hourly_forecast" -> HourlyCard(data.hourlyForecasts, backdrop, params, textColor)
                                "daily_forecast" -> DailyCard(data.dailyForecasts, backdrop, params, textColor)
                                "details" -> DetailsCard(data, backdrop, params, textColor)
                                "wind_info" -> WindCard(data, backdrop, params, textColor)
                                "visibility_pressure" -> VisPressureCard(data, backdrop, params, textColor)
                                "aqi" -> AqiCard(data.aqi, backdrop, params, textColor)
                                "sun_moon" -> SunMoonCard(data.dailyForecasts, backdrop, params, textColor)
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    // ═══ Footer ═══
                    item(key = "footer") {
                        Spacer(Modifier.height(16.dp))
                        GlassButton("刷新天气", onClick = { refresh() })
                        Spacer(Modifier.height(48.dp))
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// Hourly Card
// ═══════════════════════════════════════════

@Composable
private fun HourlyCard(
    hourly: List<HourlyForecast>,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    params: GlassParams,
    textColor: Color
) {
    GlassCard(backdrop, params, Modifier.padding(horizontal = 24.dp), title = "逐小时预报") {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(hourly.take(24)) { h ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(48.dp)
                ) {
                    BasicText(
                        h.displayTime,
                        style = TextStyle(fontSize = 12.sp, color = textColor.copy(alpha = 0.7f)),
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    WeatherIconCanvas(icon = h.icon, modifier = Modifier.size(24.dp), tint = textColor)
                    Spacer(Modifier.height(4.dp))
                    BasicText(
                        "${h.temp}°",
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor)
                    )
                    BasicText(
                        h.text,
                        style = TextStyle(fontSize = 10.sp, color = textColor.copy(alpha = 0.6f))
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// Daily Card
// ═══════════════════════════════════════════

@Composable
private fun DailyCard(
    daily: List<DailyForecast>,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    params: GlassParams,
    textColor: Color
) {
    GlassCard(backdrop, params, Modifier.padding(horizontal = 24.dp), title = "7日预报") {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            daily.forEach { d ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        d.displayDay,
                        style = TextStyle(fontSize = 13.sp, color = textColor),
                        modifier = Modifier.width(50.dp)
                    )
                    WeatherIconCanvas(icon = d.iconDay, modifier = Modifier.size(20.dp), tint = textColor)
                    Spacer(Modifier.width(4.dp))
                    BasicText(
                        d.textDay,
                        style = TextStyle(fontSize = 12.sp, color = textColor.copy(alpha = 0.7f)),
                        modifier = Modifier.weight(1f)
                    )
                    BasicText(
                        "UV ${d.uvIndex}",
                        style = TextStyle(fontSize = 11.sp, color = textColor.copy(alpha = 0.6f)),
                        modifier = Modifier.width(40.dp)
                    )
                    BasicText(
                        "${d.tempMin}°~${d.tempMax}°",
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor),
                        modifier = Modifier.width(65.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// Details Card
// ═══════════════════════════════════════════

@Composable
private fun DetailsCard(
    data: WeatherResult,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    params: GlassParams,
    textColor: Color
) {
    GlassCard(backdrop, params, Modifier.padding(horizontal = 24.dp), title = "详细信息") {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InfoCol("湿度", "${data.humidity}%", textColor)
            InfoCol("云量", "${data.cloud}%", textColor)
            InfoCol("降水", "${data.precip}mm", textColor)
            InfoCol("露点", "${data.dewPoint}°", textColor)
        }
    }
}

// ═══════════════════════════════════════════
// Wind Card
// ═══════════════════════════════════════════

@Composable
private fun WindCard(
    data: WeatherResult,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    params: GlassParams,
    textColor: Color
) {
    GlassCard(backdrop, params, Modifier.padding(horizontal = 24.dp), title = "风力信息") {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InfoCol("风向", data.windDir, textColor)
            InfoCol("风速", "${data.windSpeed}km/h", textColor)
            InfoCol("风力", "${data.windScale}级", textColor)
            InfoCol("阵风", "--", textColor)
        }
    }
}

// ═══════════════════════════════════════════
// Visibility & Pressure Card
// ═══════════════════════════════════════════

@Composable
private fun VisPressureCard(
    data: WeatherResult,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    params: GlassParams,
    textColor: Color
) {
    GlassCard(backdrop, params, Modifier.padding(horizontal = 24.dp), title = "能见度与气压") {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InfoCol("能见度", "${data.vis}km", textColor)
            InfoCol("气压", "${data.pressure}hPa", textColor)
        }
    }
}

// ═══════════════════════════════════════════
// AQI Card
// ═══════════════════════════════════════════

@Composable
private fun AqiCard(
    aqi: AqiData?,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    params: GlassParams,
    textColor: Color
) {
    if (aqi == null) return
    GlassCard(backdrop, params, Modifier.padding(horizontal = 24.dp), title = "空气质量") {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicText(
                "${aqi.aqi}",
                style = TextStyle(
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            )
            Spacer(Modifier.height(4.dp))
            BasicText(
                "${aqi.category} · ${aqi.primary.ifEmpty { "无首要污染物" }}",
                style = TextStyle(fontSize = 13.sp, color = textColor.copy(alpha = 0.8f))
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoCol("PM2.5", "%.0f".format(aqi.pm2p5), textColor)
                InfoCol("PM10", "%.0f".format(aqi.pm10), textColor)
                InfoCol("O₃", "%.0f".format(aqi.o3), textColor)
                InfoCol("NO₂", "%.0f".format(aqi.no2), textColor)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoCol("SO₂", "%.0f".format(aqi.so2), textColor)
                InfoCol("CO", "%.1f".format(aqi.co), textColor)
            }
        }
    }
}

// ═══════════════════════════════════════════
// Sun & Moon Card
// ═══════════════════════════════════════════

@Composable
private fun SunMoonCard(
    daily: List<DailyForecast>,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    params: GlassParams,
    textColor: Color
) {
    if (daily.isEmpty()) return
    val today = daily.first()
    GlassCard(backdrop, params, Modifier.padding(horizontal = 24.dp), title = "日月信息") {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InfoCol("日出", today.sunrise, textColor)
            InfoCol("日落", today.sunset, textColor)
            InfoCol("月出", today.moonrise, textColor)
            InfoCol("月落", today.moonset, textColor)
            InfoCol("月相", today.moonPhase, textColor)
        }
    }
}

// ═══════════════════════════════════════════
// Helpers
// ═══════════════════════════════════════════

@Composable
private fun SectionTitle(text: String, textColor: Color) {
    BasicText(
        text,
        style = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = textColor.copy(alpha = 0.8f)
        ),
        modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun InfoCol(label: String, value: String, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(
            value,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = textColor)
        )
        BasicText(
            label,
            style = TextStyle(fontSize = 11.sp, color = textColor.copy(alpha = 0.6f))
        )
    }
}
