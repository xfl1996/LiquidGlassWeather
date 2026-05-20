package com.kyant.backdrop.weather

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume


/**
 * Helper for getting device location using Android's built-in LocationManager.
 * No additional dependencies required.
 *
 * Falls back to Beijing coordinates (39.9042, 116.4074) on failure.
 */
object LocationHelper {

    // Default: Beijing
    private const val DEFAULT_LATITUDE = 39.9042
    private const val DEFAULT_LONGITUDE = 116.4074

    /**
     * Get the current device location.
     * Returns Pair(latitude, longitude).
     * Falls back to Beijing coordinates on any failure.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Pair<Double, Double> {
        return withContext(Dispatchers.IO) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

                val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                if (!hasGps && !hasNetwork) {
                    return@withContext Pair(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
                }

                // Priority: GPS first
                if (hasGps) {
                    val lastGps = getLastKnownLocationFromProvider(locationManager, LocationManager.GPS_PROVIDER)
                    if (lastGps != null) {
                        val age = System.currentTimeMillis() - lastGps.time
                        if (age < 5 * 60 * 1000) {
                            return@withContext Pair(lastGps.latitude, lastGps.longitude)
                        }
                    }
                    val freshGps = requestFreshLocationFromProvider(locationManager, LocationManager.GPS_PROVIDER, 10_000L)
                    if (freshGps != null) {
                        return@withContext Pair(freshGps.latitude, freshGps.longitude)
                    }
                }

                // Fallback to Network
                if (hasNetwork) {
                    val lastNet = getLastKnownLocationFromProvider(locationManager, LocationManager.NETWORK_PROVIDER)
                    if (lastNet != null) {
                        val age = System.currentTimeMillis() - lastNet.time
                        if (age < 10 * 60 * 1000) {
                            return@withContext Pair(lastNet.latitude, lastNet.longitude)
                        }
                    }
                    val freshNet = requestFreshLocationFromProvider(locationManager, LocationManager.NETWORK_PROVIDER, 8_000L)
                    if (freshNet != null) {
                        return@withContext Pair(freshNet.latitude, freshNet.longitude)
                    }
                }

                Pair(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
            } catch (e: Exception) {
                Pair(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
            }
        }
    }

    /**
     * Get a human-readable location description from coordinates.
     * Uses QWeather GeoAPI (more reliable than Android Geocoder on Chinese phones).
     */
    suspend fun getLocationDescription(context: Context, lat: Double, lon: Double): String {
        // Use QWeather reverse geocode (same API as weather data, more accurate)
        val geoName = WeatherService.reverseGeocode(lat, lon)
        if (!geoName.isNullOrEmpty()) {
            return geoName
        }

        // Fallback to Android Geocoder
        return try {
            val geocoder = Geocoder(context, Locale.CHINA)
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val city = addr.locality ?: ""
                val district = addr.subLocality ?: ""
                val admin = addr.adminArea ?: ""

                when {
                    district.isNotEmpty() && city.isNotEmpty() && district == city -> district
                    district.isNotEmpty() && city.isNotEmpty() && district.contains(city.removeSuffix("市")) -> district
                    city.isNotEmpty() -> city
                    district.isNotEmpty() -> district
                    admin.isNotEmpty() -> admin
                    else -> String.format(Locale.US, "%.2f°N, %.2f°E", lat, lon)
                }
            } else {
                String.format(Locale.US, "%.2f°N, %.2f°E", lat, lon)
            }
        } catch (e: Exception) {
            String.format(Locale.US, "%.2f°N, %.2f°E", lat, lon)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocationFromProvider(
        locationManager: LocationManager,
        provider: String
    ): Location? {
        return try {
            locationManager.getLastKnownLocation(provider)
        } catch (e: SecurityException) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocationFromProvider(
        locationManager: LocationManager,
        provider: String,
        timeoutMs: Long
    ): Location? {
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        locationManager.removeUpdates(this)
                        if (cont.isActive) cont.resume(loc)
                    }
                    @Deprecated("Deprecated in API")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }

                try {
                    locationManager.requestLocationUpdates(provider, 0L, 0f, listener)
                } catch (e: SecurityException) {
                    if (cont.isActive) cont.resume(null)
                }

                cont.invokeOnCancellation {
                    locationManager.removeUpdates(listener)
                }
            }
        }
    }
}
