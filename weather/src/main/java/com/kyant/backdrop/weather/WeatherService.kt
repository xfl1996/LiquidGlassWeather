package com.kyant.backdrop.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * Fetches weather data from QWeather (和风天气) v7 API.
 *
 * Uses direct Java HTTP (like the Flutter project's MethodChannel approach)
 * to ensure compatibility.
 */
object WeatherService {

    private const val API_KEY = "c0a200c0a7cb40ed9d1687681fbf9608"
    private const val API_HOST = "nq4t2cqrxd.re.qweatherapi.com"

    /**
     * Perform an HTTPS GET request with SSL trust-all (matches Flutter project).
     */
    private suspend fun httpGet(urlStr: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpsURLConnection

            // Trust all certificates (same as Flutter project's MainActivity.kt)
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            }), java.security.SecureRandom())
            connection.sslSocketFactory = sslContext.socketFactory
            connection.hostnameVerifier = HostnameVerifier { _, _ -> true }

            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "LiquidGlassWeather/1.0")

            val statusCode = connection.responseCode
            if (statusCode != 200) {
                connection.disconnect()
                return@withContext null
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val body = reader.readText()
            reader.close()
            connection.disconnect()
            body
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Fetch a QWeather API endpoint and return parsed JSON.
     */
    private suspend fun fetchApi(
        path: String,
        vararg extraParams: Pair<String, String>
    ): JSONObject? {
        val params = mutableListOf("key" to API_KEY)
        params.addAll(extraParams)
        val qs = params.joinToString("&") { "${it.first}=${it.second}" }
        val url = "https://$API_HOST$path?$qs"

        val body = httpGet(url) ?: return null
        return try {
            val data = JSONObject(body)
            if (data.optString("code") != "200") null else data
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Fetch current weather for the given location.
     */
    suspend fun fetchWeatherNow(
        latitude: Double,
        longitude: Double
    ): WeatherNow? {
        val location = "$longitude,$latitude"
        val json = fetchApi("/v7/weather/now", "location" to location) ?: return null
        val now = json.optJSONObject("now") ?: return null
        return WeatherNow.fromJsonObject(now)
    }

    /**
     * Fetch 24-hour hourly forecast.
     */
    suspend fun fetchHourlyForecast(
        latitude: Double,
        longitude: Double
    ): List<HourlyForecast> {
        val location = "$longitude,$latitude"
        val json = fetchApi("/v7/weather/24h", "location" to location) ?: return emptyList()
        val hourly = json.optJSONArray("hourly") ?: return emptyList()
        return (0 until hourly.length()).mapNotNull { i ->
            val obj = hourly.optJSONObject(i) ?: return@mapNotNull null
            HourlyForecast.fromJsonObject(obj)
        }
    }

    /**
     * Fetch 10-day daily forecast.
     */
    suspend fun fetchDailyForecast(
        latitude: Double,
        longitude: Double
    ): List<DailyForecast> {
        val location = "$longitude,$latitude"
        val json = fetchApi("/v7/weather/10d", "location" to location) ?: return emptyList()
        val daily = json.optJSONArray("daily") ?: return emptyList()
        return (0 until daily.length()).mapNotNull { i ->
            val obj = daily.optJSONObject(i) ?: return@mapNotNull null
            DailyForecast.fromJsonObject(obj)
        }
    }

    /**
     * Fetch AQI data for the given location.
     */
    suspend fun fetchAqi(
        latitude: Double,
        longitude: Double
    ): AqiData? {
        val location = "$longitude,$latitude"
        val json = fetchApi("/v7/air/now", "location" to location) ?: return null
        val now = json.optJSONObject("now") ?: return null
        return AqiData.fromJsonObject(now)
    }

    /**
     * Search cities by name using QWeather GeoAPI.
     * Uses same API_HOST as weather data (like Flutter project).
     * Returns a list of matching locations with name, admin area, lat/lon.
     */
    suspend fun searchCity(name: String): List<CityResult> {
        val json = fetchApi("/geo/v2/city/lookup", "location" to name, "number" to "10") ?: return emptyList()
        val locationArr = json.optJSONArray("location") ?: return emptyList()
        return (0 until locationArr.length()).mapNotNull { i ->
            val obj = locationArr.optJSONObject(i) ?: return@mapNotNull null
            CityResult(
                name = obj.optString("name", ""),
                adminArea = obj.optString("adm1", obj.optString("adm2", "")),
                latitude = obj.optString("lat", "0").toDoubleOrNull() ?: 0.0,
                longitude = obj.optString("lon", "0").toDoubleOrNull() ?: 0.0,
                country = obj.optString("country", ""),
                tz = obj.optString("tz", "")
            )
        }
    }
}

/**
 * City search result from geocoding API.
 */
data class CityResult(
    val name: String,
    val adminArea: String,
    val latitude: Double,
    val longitude: Double,
    val country: String,
    val tz: String
)
