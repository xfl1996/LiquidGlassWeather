package com.kyant.backdrop.weather

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Weather data models for QWeather (和风天气) API responses.
 * Includes ALL available API parameters.
 */

enum class WeatherCondition {
    SUNNY, CLOUDY, PARTLY_CLOUDY, RAINY, THUNDERSTORM, RAIN_THUNDER, FOGGY, SNOWY
}

/** Current weather conditions — /v7/weather/now */
data class WeatherNow(
    val temp: Int,
    val feelsLike: Int,
    val icon: WeatherCondition,
    val text: String,
    val windDir: String,
    val windScale: String,
    val windSpeed: String,
    val humidity: Int,
    val precip: Float,           // mm — precipitation
    val pressure: Int,           // hPa
    val vis: Int,                // km — visibility
    val cloud: Int,              // % — cloud cover
    val dewPoint: Int            // °C
) {
    companion object {
        fun fromJsonObject(json: JSONObject): WeatherNow {
            val text = json.optString("text", "未知")
            return WeatherNow(
                temp = json.optString("temp", "0").toIntOrNull() ?: 0,
                feelsLike = json.optString("feelsLike", "0").toIntOrNull() ?: 0,
                icon = conditionToIcon(text),
                text = text,
                windDir = json.optString("windDir", ""),
                windScale = json.optString("windScale", "0"),
                windSpeed = json.optString("windSpeed", "0"),
                humidity = json.optString("humidity", "0").toIntOrNull() ?: 0,
                precip = json.optString("precip", "0").toFloatOrNull() ?: 0f,
                pressure = json.optString("pressure", "0").toIntOrNull() ?: 0,
                vis = json.optString("vis", "0").toIntOrNull() ?: 0,
                cloud = json.optString("cloud", "0").toIntOrNull() ?: 0,
                dewPoint = json.optString("dewPoint", "0").toIntOrNull() ?: 0
            )
        }
    }
}

/** Hourly forecast — /v7/weather/24h or /v7/weather/72h */
data class HourlyForecast(
    val fxTime: String,
    val temp: Int,
    val feelsLike: Int,
    val icon: WeatherCondition,
    val text: String,
    val windDir: String,
    val windScale: String,
    val windSpeed: String,
    val humidity: Int,
    val pop: Int,                // % — precipitation probability
    val precip: Float,           // mm
    val pressure: Int,
    val cloud: Int,              // %
    val vis: Int,                // km
    val dewPoint: Int
) {
    val displayTime: String
        get() {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mmXXX", Locale.getDefault())
                sdf.timeZone = TimeZone.getDefault()
                val date = sdf.parse(fxTime) ?: return fxTime
                val cal = Calendar.getInstance()
                val now = Calendar.getInstance()
                cal.time = date
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val dayDiff = (cal.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR) +
                    (cal.get(Calendar.YEAR) - now.get(Calendar.YEAR)) * 365)
                when {
                    dayDiff == 0 -> "${hour}时"
                    dayDiff == 1 -> "明天${hour}时"
                    else -> "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)} ${hour}时"
                }
            } catch (e: Exception) {
                fxTime
            }
        }

    companion object {
        fun fromJsonObject(json: JSONObject): HourlyForecast {
            val text = json.optString("text", "未知")
            return HourlyForecast(
                fxTime = json.optString("fxTime", ""),
                temp = json.optString("temp", "0").toIntOrNull() ?: 0,
                feelsLike = json.optString("feelsLike", "0").toIntOrNull() ?: 0,
                icon = conditionToIcon(text),
                text = text,
                windDir = json.optString("windDir", ""),
                windScale = json.optString("windScale", "0"),
                windSpeed = json.optString("windSpeed", "0"),
                humidity = json.optString("humidity", "0").toIntOrNull() ?: 0,
                pop = json.optString("pop", "0").toIntOrNull() ?: 0,
                precip = json.optString("precip", "0").toFloatOrNull() ?: 0f,
                pressure = json.optString("pressure", "0").toIntOrNull() ?: 0,
                cloud = json.optString("cloud", "0").toIntOrNull() ?: 0,
                vis = json.optString("vis", "0").toIntOrNull() ?: 0,
                dewPoint = json.optString("dewPoint", "0").toIntOrNull() ?: 0
            )
        }
    }
}

/** Daily forecast — /v7/weather/3d or /v7/weather/7d */
data class DailyForecast(
    val fxDate: String,
    val tempMin: Int,
    val tempMax: Int,
    val iconDay: WeatherCondition,
    val iconNight: WeatherCondition,
    val textDay: String,
    val textNight: String,
    val windDirDay: String,
    val windScaleDay: String,
    val windSpeedDay: String,
    val windDirNight: String,
    val windScaleNight: String,
    val windSpeedNight: String,
    val humidity: Int,
    val precip: Float,           // mm
    val pressure: Int,
    val vis: Int,
    val cloud: Int,
    val uvIndex: Int,
    val sunrise: String,
    val sunset: String,
    val moonrise: String,
    val moonset: String,
    val moonPhase: String        // e.g. "上弦月"
) {
    val displayDay: String
        get() {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(fxDate) ?: return fxDate
                val cal = Calendar.getInstance()
                cal.time = date
                val today = Calendar.getInstance()
                val dayDiff = (cal.get(Calendar.DAY_OF_YEAR) - today.get(Calendar.DAY_OF_YEAR) +
                    (cal.get(Calendar.YEAR) - today.get(Calendar.YEAR)) * 365)
                when (dayDiff) {
                    0 -> "今天"
                    1 -> "明天"
                    2 -> "后天"
                    else -> {
                        val dayNames = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
                        dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
                    }
                }
            } catch (e: Exception) {
                fxDate
            }
        }

    companion object {
        fun fromJsonObject(json: JSONObject): DailyForecast {
            val textDay = json.optString("textDay", "未知")
            val textNight = json.optString("textNight", "未知")
            return DailyForecast(
                fxDate = json.optString("fxDate", ""),
                tempMin = json.optString("tempMin", "0").toIntOrNull() ?: 0,
                tempMax = json.optString("tempMax", "0").toIntOrNull() ?: 0,
                iconDay = conditionToIcon(textDay),
                iconNight = conditionToIcon(textNight),
                textDay = textDay,
                textNight = textNight,
                windDirDay = json.optString("windDirDay", ""),
                windScaleDay = json.optString("windScaleDay", "0"),
                windSpeedDay = json.optString("windSpeedDay", "0"),
                windDirNight = json.optString("windDirNight", ""),
                windScaleNight = json.optString("windScaleNight", "0"),
                windSpeedNight = json.optString("windSpeedNight", "0"),
                humidity = json.optString("humidity", "0").toIntOrNull() ?: 0,
                precip = json.optString("precip", "0").toFloatOrNull() ?: 0f,
                pressure = json.optString("pressure", "0").toIntOrNull() ?: 0,
                vis = json.optString("vis", "0").toIntOrNull() ?: 0,
                cloud = json.optString("cloud", "0").toIntOrNull() ?: 0,
                uvIndex = json.optString("uvIndex", "0").toIntOrNull() ?: 0,
                sunrise = json.optString("sunrise", ""),
                sunset = json.optString("sunset", ""),
                moonrise = json.optString("moonrise", ""),
                moonset = json.optString("moonset", ""),
                moonPhase = json.optString("moonPhase", "")
            )
        }
    }
}

/** Air quality — /airquality/v1/current/{locationId} or free endpoint */
data class AqiData(
    val aqi: Int,
    val category: String,
    val primary: String,
    val pm2p5: Float = 0f,
    val pm10: Float = 0f,
    val so2: Float = 0f,
    val no2: Float = 0f,
    val co: Float = 0f,
    val o3: Float = 0f
) {
    val level: String
        get() = when {
            aqi <= 50 -> "优"
            aqi <= 100 -> "良"
            aqi <= 150 -> "轻度污染"
            aqi <= 200 -> "中度污染"
            aqi <= 300 -> "重度污染"
            else -> "严重污染"
        }

    companion object {
        fun fromJsonObject(json: JSONObject): AqiData {
            return AqiData(
                aqi = json.optString("aqi", "0").toIntOrNull() ?: 0,
                category = json.optString("category", "未知"),
                primary = json.optString("primary", ""),
                pm2p5 = json.optString("pm2p5", "0").toFloatOrNull() ?: 0f,
                pm10 = json.optString("pm10", "0").toFloatOrNull() ?: 0f,
                so2 = json.optString("so2", "0").toFloatOrNull() ?: 0f,
                no2 = json.optString("no2", "0").toFloatOrNull() ?: 0f,
                co = json.optString("co", "0").toFloatOrNull() ?: 0f,
                o3 = json.optString("o3", "0").toFloatOrNull() ?: 0f
            )
        }
    }
}

/** Mapping from weather text to display icon. */
fun conditionToIcon(text: String): WeatherCondition {
    return when {
        text.contains("雷") && (text.contains("雨") || text.contains("暴")) -> WeatherCondition.THUNDERSTORM
        text.contains("雨") && text.contains("雷") -> WeatherCondition.RAIN_THUNDER
        text.contains("暴雨") || text.contains("大暴雨") || text.contains("特大暴雨") -> WeatherCondition.RAIN_THUNDER
        text.contains("阵雨") || text.contains("雷阵雨") -> WeatherCondition.THUNDERSTORM
        text.contains("雨") -> WeatherCondition.RAINY
        text.contains("雪") -> WeatherCondition.SNOWY
        text.contains("多云") || text.contains("阴") -> WeatherCondition.CLOUDY
        text.contains("晴") && text.contains("转") -> WeatherCondition.PARTLY_CLOUDY
        text.contains("晴") -> WeatherCondition.SUNNY
        text.contains("雾") || text.contains("霾") || text.contains("沙") -> WeatherCondition.FOGGY
        else -> WeatherCondition.PARTLY_CLOUDY
    }
}

/** Mapping from weather condition to text color for readability. */
fun conditionToTextColor(condition: WeatherCondition?): androidx.compose.ui.graphics.Color {
    return when (condition) {
        WeatherCondition.SUNNY -> androidx.compose.ui.graphics.Color(0xFF2D3156)
        WeatherCondition.FOGGY -> androidx.compose.ui.graphics.Color(0xFF4A3B52)
        WeatherCondition.SNOWY -> androidx.compose.ui.graphics.Color(0xFF1B3A4B)
        WeatherCondition.CLOUDY -> androidx.compose.ui.graphics.Color(0xFF272332)
        WeatherCondition.PARTLY_CLOUDY -> androidx.compose.ui.graphics.Color(0xFF352C43)
        WeatherCondition.THUNDERSTORM -> androidx.compose.ui.graphics.Color(0xFFEBE4F5)
        WeatherCondition.RAIN_THUNDER -> androidx.compose.ui.graphics.Color(0xFFEBE4F5)
        WeatherCondition.RAINY -> androidx.compose.ui.graphics.Color(0xFFDCE8F0)
        null -> androidx.compose.ui.graphics.Color(0xFF272332)
    }
}

/** Default hex color for each weather condition (for settings UI). */
val WEATHER_TEXT_COLOR_DEFAULTS: Map<WeatherCondition, String> = mapOf(
    WeatherCondition.SUNNY to "#2D3156",
    WeatherCondition.FOGGY to "#4A3B52",
    WeatherCondition.SNOWY to "#1B3A4B",
    WeatherCondition.CLOUDY to "#272332",
    WeatherCondition.PARTLY_CLOUDY to "#352C43",
    WeatherCondition.THUNDERSTORM to "#EBE4F5",
    WeatherCondition.RAIN_THUNDER to "#EBE4F5",
    WeatherCondition.RAINY to "#DCE8F0"
)

/** Display name for each weather condition (for settings UI). */
val WEATHER_CONDITION_DISPLAY_NAMES: Map<WeatherCondition, String> = mapOf(
    WeatherCondition.SUNNY to "晴天",
    WeatherCondition.FOGGY to "雾天",
    WeatherCondition.SNOWY to "雪天",
    WeatherCondition.CLOUDY to "阴天",
    WeatherCondition.PARTLY_CLOUDY to "多云转晴",
    WeatherCondition.THUNDERSTORM to "雷暴",
    WeatherCondition.RAIN_THUNDER to "雷雨",
    WeatherCondition.RAINY to "雨天"
)
