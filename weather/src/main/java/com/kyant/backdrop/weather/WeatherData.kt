package com.kyant.backdrop.weather

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


/**
 * Weather data layer.
 * Fetches real weather data from QWeather API with local same-day caching.
 */

data class WeatherResult(
    val cityName: String,
    val latitude: Double,
    val longitude: Double,
    // Current conditions
    val currentTemp: Int,
    val feelsLike: Int,
    val weatherDescription: String,
    val weatherIcon: WeatherCondition,
    val humidity: Int,
    val precip: Float,
    val pressure: Int,
    val vis: Int,
    val cloud: Int,
    val dewPoint: Int,
    // Wind
    val windDir: String,
    val windSpeed: String,
    val windScale: String,
    // AQI
    val aqi: AqiData?,
    // Forecasts
    val hourlyForecasts: List<HourlyForecast>,
    val dailyForecasts: List<DailyForecast>
) {
    /** Serialize to JSON string for caching. */
    fun toJson(): String {
        val root = JSONObject()
        root.put("cityName", cityName)
        root.put("latitude", latitude)
        root.put("longitude", longitude)
        root.put("currentTemp", currentTemp)
        root.put("feelsLike", feelsLike)
        root.put("weatherDescription", weatherDescription)
        root.put("weatherIcon", weatherIcon.name)
        root.put("humidity", humidity)
        root.put("precip", precip.toDouble())
        root.put("pressure", pressure)
        root.put("vis", vis)
        root.put("cloud", cloud)
        root.put("dewPoint", dewPoint)
        root.put("windDir", windDir)
        root.put("windSpeed", windSpeed)
        root.put("windScale", windScale)

        // AQI
        if (aqi != null) {
            val aqiObj = JSONObject()
            aqiObj.put("aqi", aqi.aqi)
            aqiObj.put("category", aqi.category)
            aqiObj.put("primary", aqi.primary)
            aqiObj.put("pm2p5", aqi.pm2p5.toDouble())
            aqiObj.put("pm10", aqi.pm10.toDouble())
            aqiObj.put("so2", aqi.so2.toDouble())
            aqiObj.put("no2", aqi.no2.toDouble())
            aqiObj.put("co", aqi.co.toDouble())
            aqiObj.put("o3", aqi.o3.toDouble())
            root.put("aqi", aqiObj)
        }

        // Hourly forecasts
        val hourlyArr = JSONArray()
        for (h in hourlyForecasts) {
            val hObj = JSONObject()
            hObj.put("fxTime", h.fxTime)
            hObj.put("temp", h.temp)
            hObj.put("feelsLike", h.feelsLike)
            hObj.put("icon", h.icon.name)
            hObj.put("text", h.text)
            hObj.put("windDir", h.windDir)
            hObj.put("windScale", h.windScale)
            hObj.put("windSpeed", h.windSpeed)
            hObj.put("humidity", h.humidity)
            hObj.put("pop", h.pop)
            hObj.put("precip", h.precip.toDouble())
            hObj.put("pressure", h.pressure)
            hObj.put("cloud", h.cloud)
            hObj.put("vis", h.vis)
            hObj.put("dewPoint", h.dewPoint)
            hourlyArr.put(hObj)
        }
        root.put("hourlyForecasts", hourlyArr)

        // Daily forecasts
        val dailyArr = JSONArray()
        for (d in dailyForecasts) {
            val dObj = JSONObject()
            dObj.put("fxDate", d.fxDate)
            dObj.put("tempMin", d.tempMin)
            dObj.put("tempMax", d.tempMax)
            dObj.put("iconDay", d.iconDay.name)
            dObj.put("textDay", d.textDay)
            dObj.put("iconNight", d.iconNight.name)
            dObj.put("textNight", d.textNight)
            dObj.put("windDirDay", d.windDirDay)
            dObj.put("windScaleDay", d.windScaleDay)
            dObj.put("windSpeedDay", d.windSpeedDay)
            dObj.put("windDirNight", d.windDirNight)
            dObj.put("windScaleNight", d.windScaleNight)
            dObj.put("windSpeedNight", d.windSpeedNight)
            dObj.put("humidity", d.humidity)
            dObj.put("precip", d.precip.toDouble())
            dObj.put("pressure", d.pressure)
            dObj.put("vis", d.vis)
            dObj.put("cloud", d.cloud)
            dObj.put("uvIndex", d.uvIndex)
            dObj.put("sunrise", d.sunrise)
            dObj.put("sunset", d.sunset)
            dObj.put("moonrise", d.moonrise)
            dObj.put("moonset", d.moonset)
            dObj.put("moonPhase", d.moonPhase)
            dailyArr.put(dObj)
        }
        root.put("dailyForecasts", dailyArr)

        return root.toString()
    }

    companion object {
        /** Deserialize from JSON string. */
        fun fromJson(json: String): WeatherResult? {
            return try {
                val root = JSONObject(json)
                val aqiObj = root.optJSONObject("aqi")
                val aqi = if (aqiObj != null) {
                    AqiData(
                        aqi = aqiObj.optInt("aqi", 0),
                        category = aqiObj.optString("category", ""),
                        primary = aqiObj.optString("primary", ""),
                        pm2p5 = aqiObj.optDouble("pm2p5", 0.0).toFloat(),
                        pm10 = aqiObj.optDouble("pm10", 0.0).toFloat(),
                        so2 = aqiObj.optDouble("so2", 0.0).toFloat(),
                        no2 = aqiObj.optDouble("no2", 0.0).toFloat(),
                        o3 = aqiObj.optDouble("o3", 0.0).toFloat(),
                        co = aqiObj.optDouble("co", 0.0).toFloat()
                    )
                } else null

                val hourlyArr = root.optJSONArray("hourlyForecasts")
                val hourly = if (hourlyArr != null) {
                    (0 until hourlyArr.length()).map { i ->
                        val h = hourlyArr.getJSONObject(i)
                        HourlyForecast(
                            fxTime = h.optString("fxTime", ""),
                            temp = h.optInt("temp", 0),
                            feelsLike = h.optInt("feelsLike", 0),
                            icon = WeatherCondition.valueOf(h.optString("icon", "SUNNY")),
                            text = h.optString("text", ""),
                            windDir = h.optString("windDir", ""),
                            windScale = h.optString("windScale", ""),
                            windSpeed = h.optString("windSpeed", ""),
                            humidity = h.optInt("humidity", 0),
                            pop = h.optInt("pop", 0),
                            precip = h.optDouble("precip", 0.0).toFloat(),
                            pressure = h.optInt("pressure", 0),
                            cloud = h.optInt("cloud", 0),
                            vis = h.optInt("vis", 0),
                            dewPoint = h.optInt("dewPoint", 0)
                        )
                    }
                } else emptyList()

                val dailyArr = root.optJSONArray("dailyForecasts")
                val daily = if (dailyArr != null) {
                    (0 until dailyArr.length()).map { i ->
                        val d = dailyArr.getJSONObject(i)
                        DailyForecast(
                            fxDate = d.optString("fxDate", ""),
                            tempMin = d.optInt("tempMin", 0),
                            tempMax = d.optInt("tempMax", 0),
                            iconDay = WeatherCondition.valueOf(d.optString("iconDay", "SUNNY")),
                            textDay = d.optString("textDay", ""),
                            iconNight = WeatherCondition.valueOf(d.optString("iconNight", "SUNNY")),
                            textNight = d.optString("textNight", ""),
                            windDirDay = d.optString("windDirDay", ""),
                            windScaleDay = d.optString("windScaleDay", ""),
                            windSpeedDay = d.optString("windSpeedDay", ""),
                            windDirNight = d.optString("windDirNight", ""),
                            windScaleNight = d.optString("windScaleNight", ""),
                            windSpeedNight = d.optString("windSpeedNight", ""),
                            humidity = d.optInt("humidity", 0),
                            precip = d.optDouble("precip", 0.0).toFloat(),
                            pressure = d.optInt("pressure", 0),
                            vis = d.optInt("vis", 0),
                            cloud = d.optInt("cloud", 0),
                            uvIndex = d.optInt("uvIndex", 0),
                            sunrise = d.optString("sunrise", ""),
                            sunset = d.optString("sunset", ""),
                            moonrise = d.optString("moonrise", ""),
                            moonset = d.optString("moonset", ""),
                            moonPhase = d.optString("moonPhase", "")
                        )
                    }
                } else emptyList()

                WeatherResult(
                    cityName = root.optString("cityName", ""),
                    latitude = root.optDouble("latitude", 0.0),
                    longitude = root.optDouble("longitude", 0.0),
                    currentTemp = root.optInt("currentTemp", 0),
                    feelsLike = root.optInt("feelsLike", 0),
                    weatherDescription = root.optString("weatherDescription", ""),
                    weatherIcon = WeatherCondition.valueOf(root.optString("weatherIcon", "SUNNY")),
                    humidity = root.optInt("humidity", 0),
                    precip = root.optDouble("precip", 0.0).toFloat(),
                    pressure = root.optInt("pressure", 0),
                    vis = root.optInt("vis", 0),
                    cloud = root.optInt("cloud", 0),
                    dewPoint = root.optInt("dewPoint", 0),
                    windDir = root.optString("windDir", ""),
                    windSpeed = root.optString("windSpeed", ""),
                    windScale = root.optString("windScale", ""),
                    aqi = aqi,
                    hourlyForecasts = hourly,
                    dailyForecasts = daily
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}

object WeatherRepository {

    private const val PREFS_NAME = "weather_cache"
    private const val KEY_CACHE_JSON = "cached_weather_json"
    private const val KEY_CACHE_DATE = "cached_weather_date"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun todayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(System.currentTimeMillis())
    }

    /**
     * Fetch weather data. Uses cache if available from today.
     * Set forceRefresh = true to bypass cache (e.g. manual pull-to-refresh).
     */
    suspend fun fetchAllWeather(context: Context, forceRefresh: Boolean = false): Result<WeatherResult> {
        val prefs = getPrefs(context)

        // Try reading cache (skip if forceRefresh)
        if (!forceRefresh) {
            val cachedDate = prefs.getString(KEY_CACHE_DATE, null)
            val cachedJson = prefs.getString(KEY_CACHE_JSON, null)
            if (cachedDate == todayString() && !cachedJson.isNullOrEmpty()) {
                val cached = WeatherResult.fromJson(cachedJson)
                if (cached != null) {
                    return Result.success(cached)
                }
            }
        }

        // Fetch from server using detected location
        return try {
            val (lat, lon) = LocationHelper.getCurrentLocation(context)
            fetchWeatherForCoords(context, lat, lon)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Load weather from local cache only. Never triggers GPS or network.
     * Returns null if no cached data exists.
     */
    fun loadCachedOnly(context: Context): WeatherResult? {
        val prefs = getPrefs(context)
        val cachedDate = prefs.getString(KEY_CACHE_DATE, null)
        val cachedJson = prefs.getString(KEY_CACHE_JSON, null)
        if (cachedDate == todayString() && !cachedJson.isNullOrEmpty()) {
            val result = WeatherResult.fromJson(cachedJson)
            return result
        }
        // If no same-day cache, try any cached data (stale is better than nothing)
        if (!cachedJson.isNullOrEmpty()) {
            val result = WeatherResult.fromJson(cachedJson)
            return result
        }
        return null
    }

    /**
     * Fetch weather for a specific city (searched by user).
     * Always fetches fresh data, no caching.
     */
    suspend fun fetchWeatherForCity(
        context: Context,
        cityName: String,
        latitude: Double,
        longitude: Double
    ): Result<WeatherResult> {
        return try {
            val result = coroutineScope {
                val nowDeferred = async { WeatherService.fetchWeatherNow(latitude, longitude) }
                val hourlyDeferred = async { WeatherService.fetchHourlyForecast(latitude, longitude) }
                val dailyDeferred = async { WeatherService.fetchDailyForecast(latitude, longitude) }
                val aqiDeferred = async { WeatherService.fetchAqi(latitude, longitude) }

                val now = nowDeferred.await()
                val hourly = hourlyDeferred.await()
                val daily = dailyDeferred.await()
                val aqi = aqiDeferred.await()

                if (now == null) {
                    return@coroutineScope Result.failure(Exception("无法获取天气数据"))
                }

                val weatherResult = WeatherResult(
                    cityName = cityName,
                    latitude = latitude,
                    longitude = longitude,
                    currentTemp = now.temp,
                    feelsLike = now.feelsLike,
                    weatherDescription = now.text,
                    weatherIcon = now.icon,
                    humidity = now.humidity,
                    precip = now.precip,
                    pressure = now.pressure,
                    vis = now.vis,
                    cloud = now.cloud,
                    dewPoint = now.dewPoint,
                    windDir = now.windDir,
                    windSpeed = now.windSpeed,
                    windScale = now.windScale,
                    aqi = aqi,
                    hourlyForecasts = hourly,
                    dailyForecasts = daily
                )
                Result.success(weatherResult)
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun fetchWeatherForCoords(
        context: Context,
        lat: Double,
        lon: Double
    ): Result<WeatherResult> {
        val prefs = getPrefs(context)
        return try {
            val result = coroutineScope {
                val nowDeferred = async { WeatherService.fetchWeatherNow(lat, lon) }
                val hourlyDeferred = async { WeatherService.fetchHourlyForecast(lat, lon) }
                val dailyDeferred = async { WeatherService.fetchDailyForecast(lat, lon) }
                val aqiDeferred = async { WeatherService.fetchAqi(lat, lon) }

                val now = nowDeferred.await()
                val hourly = hourlyDeferred.await()
                val daily = dailyDeferred.await()
                val aqi = aqiDeferred.await()

                val cityName = LocationHelper.getLocationDescription(context, lat, lon)

                if (now == null) {
                    return@coroutineScope Result.failure(
                        Exception("无法获取天气数据，请检查网络连接")
                    )
                }

                val weatherResult = WeatherResult(
                    cityName = cityName,
                    latitude = lat,
                    longitude = lon,
                    currentTemp = now.temp,
                    feelsLike = now.feelsLike,
                    weatherDescription = now.text,
                    weatherIcon = now.icon,
                    humidity = now.humidity,
                    precip = now.precip,
                    pressure = now.pressure,
                    vis = now.vis,
                    cloud = now.cloud,
                    dewPoint = now.dewPoint,
                    windDir = now.windDir,
                    windSpeed = now.windSpeed,
                    windScale = now.windScale,
                    aqi = aqi,
                    hourlyForecasts = hourly,
                    dailyForecasts = daily
                )

                // Cache the result
                prefs.edit()
                    .putString(KEY_CACHE_JSON, weatherResult.toJson())
                    .putString(KEY_CACHE_DATE, todayString())
                    .apply()

                Result.success(weatherResult)
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
