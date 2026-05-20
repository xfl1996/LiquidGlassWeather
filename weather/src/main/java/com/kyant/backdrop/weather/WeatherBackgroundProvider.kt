package com.kyant.backdrop.weather

import androidx.annotation.DrawableRes
import com.kyant.backdrop.weather.R

/**
 * Provides background drawable resources based on the current weather condition.
 * When multiple images exist for one condition, picks one randomly.
 * Falls back to the default wallpaper_light for unknown/unmapped conditions.
 */
object WeatherBackgroundProvider {

    /** Map each WeatherCondition to a pool of drawable resource IDs. */
    @DrawableRes
    private val backgroundMap: Map<WeatherCondition, List<Int>> = mapOf(
        WeatherCondition.SUNNY to listOf(
            R.drawable.sunny
        ),
        WeatherCondition.CLOUDY to listOf(
            R.drawable.overcast_1,
            R.drawable.overcast_2,
            R.drawable.overcast_3
        ),
        WeatherCondition.PARTLY_CLOUDY to listOf(
            R.drawable.overcast_1,
            R.drawable.overcast_2,
            R.drawable.overcast_3
        ),
        WeatherCondition.FOGGY to listOf(
            R.drawable.foggy_1,
            R.drawable.foggy_2
        ),
        WeatherCondition.SNOWY to listOf(
            R.drawable.snow_1,
            R.drawable.snow_2
        ),
        WeatherCondition.RAINY to listOf(
            R.drawable.rainy_1,
            R.drawable.rainy_2
        ),
        WeatherCondition.THUNDERSTORM to listOf(
            R.drawable.thunder
        ),
        WeatherCondition.RAIN_THUNDER to listOf(
            R.drawable.thunder
        )
    )

    /** Default background when no weather-specific image is available. */
    @DrawableRes
    private val defaultBackground = R.drawable.wallpaper_light

    /**
     * Returns a drawable resource ID for the given weather condition.
     * Randomly selects if multiple images are available.
     */
    @DrawableRes
    fun getBackgroundResource(condition: WeatherCondition): Int {
        val candidates = backgroundMap[condition]
        return if (!candidates.isNullOrEmpty()) {
            candidates.random()
        } else {
            defaultBackground
        }
    }

    /**
     * Returns a drawable resource ID for the given nullable weather condition.
     * Falls back to default when condition is null.
     */
    @JvmStatic
    @DrawableRes
    fun getBackgroundResourceOrNull(condition: WeatherCondition?): Int {
        return if (condition != null) getBackgroundResource(condition) else defaultBackground
    }
}
