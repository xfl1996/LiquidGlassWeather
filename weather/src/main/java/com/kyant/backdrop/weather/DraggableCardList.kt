package com.kyant.backdrop.weather

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * All available card IDs in default display order.
 */
val ALL_CARD_IDS = listOf(
    "hourly_forecast",
    "daily_forecast",
    "details",
    "wind_info",
    "visibility_pressure",
    "aqi",
    "sun_moon"
)

val CARD_TITLES = mapOf(
    "hourly_forecast" to "逐时预报",
    "daily_forecast" to "7日预报",
    "details" to "详细信息",
    "wind_info" to "风力信息",
    "visibility_pressure" to "能见度与气压",
    "aqi" to "空气质量",
    "sun_moon" to "日月信息"
)

/**
 * Manages card visibility, display order, and per-card glass params via SharedPreferences.
 *
 * Stored as JSON:
 * { "order": [...], "hidden": [...], "globalParams": {...}, "cardParams": { "hourly_forecast": {...}, ... } }
 */
class CardSettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("weather_card_settings", Context.MODE_PRIVATE)

    private val KEY_SETTINGS = "card_settings_v3"

    data class Settings(
        val order: List<String>,
        val hidden: Set<String>,
        val globalParams: GlassParams = GlassParams(),
        val cardParams: Map<String, GlassParams> = emptyMap(),
        val textColorOverrides: Map<String, String> = emptyMap()  // key = WeatherCondition.name, value = "#RRGGBB"
    )

    fun load(): Settings {
        val json = prefs.getString(KEY_SETTINGS, null)
        if (json != null) {
            try {
                val obj = JSONObject(json)
                val orderArr = obj.getJSONArray("order")
                val order = mutableListOf<String>()
                for (i in 0 until orderArr.length()) {
                    order.add(orderArr.getString(i))
                }
                // Add any new cards not in saved order
                for (id in ALL_CARD_IDS) {
                    if (id !in order) order.add(id)
                }
                val hiddenArr = obj.optJSONArray("hidden")
                val hidden = mutableSetOf<String>()
                if (hiddenArr != null) {
                    for (i in 0 until hiddenArr.length()) {
                        hidden.add(hiddenArr.getString(i))
                    }
                }
                val globalParams = obj.optJSONObject("globalParams")?.let { parseGlassParams(it) } ?: GlassParams()
                val cardParamsObj = obj.optJSONObject("cardParams")
                val cardParams = mutableMapOf<String, GlassParams>()
                if (cardParamsObj != null) {
                    for (key in cardParamsObj.keys()) {
                        cardParams[key] = parseGlassParams(cardParamsObj.getJSONObject(key))
                    }
                }
                val textColorRaw = obj.optJSONObject("textColorOverrides")
                val textColorOverrides = mutableMapOf<String, String>()
                if (textColorRaw != null) {
                    for (key in textColorRaw.keys()) {
                        val v = textColorRaw.optString(key, "")
                        if (v.isNotEmpty()) textColorOverrides[key] = v
                    }
                }
                return Settings(order, hidden, globalParams, cardParams, textColorOverrides)
            } catch (_: Exception) {
                // fall through
            }
        }
        return Settings(ALL_CARD_IDS, emptySet())
    }

    fun save(settings: Settings) {
        val obj = JSONObject()
        val orderArr = JSONArray()
        settings.order.forEach { orderArr.put(it) }
        obj.put("order", orderArr)
        val hiddenArr = JSONArray()
        settings.hidden.forEach { hiddenArr.put(it) }
        obj.put("hidden", hiddenArr)
        obj.put("globalParams", glassParamsToJson(settings.globalParams))
        val cardParamsObj = JSONObject()
        settings.cardParams.forEach { (k, v) -> cardParamsObj.put(k, glassParamsToJson(v)) }
        obj.put("cardParams", cardParamsObj)
        if (settings.textColorOverrides.isNotEmpty()) {
            val tObj = JSONObject()
            settings.textColorOverrides.forEach { (k, v) -> tObj.put(k, v) }
            obj.put("textColorOverrides", tObj)
        } else {
            obj.put("textColorOverrides", JSONObject.NULL)
        }
        prefs.edit().putString(KEY_SETTINGS, obj.toString()).apply()
    }

    /** Get the ordered list of visible card IDs. */
    fun getVisibleCards(): List<String> {
        val settings = load()
        return settings.order.filter { it !in settings.hidden }
    }

    /** Resolve params for a specific card: per-card override > global */
    fun resolveParams(cardId: String): GlassParams {
        val settings = load()
        return settings.cardParams[cardId] ?: settings.globalParams
    }

    private fun parseGlassParams(obj: JSONObject): GlassParams = GlassParams(
        blur = obj.optDouble("blur", 0.0).toFloat(),
        refH = obj.optDouble("refH", 17.0).toFloat(),
        refA = obj.optDouble("refA", 128.0).toFloat(),
        corner = obj.optDouble("corner", 43.0).toFloat(),
        cardAlpha = obj.optDouble("cardAlpha", 100.0).toFloat()
    )

    private fun glassParamsToJson(p: GlassParams): JSONObject = JSONObject().apply {
        put("blur", p.blur.toDouble())
        put("refH", p.refH.toDouble())
        put("refA", p.refA.toDouble())
        put("corner", p.corner.toDouble())
        put("cardAlpha", p.cardAlpha.toDouble())
    }
}
