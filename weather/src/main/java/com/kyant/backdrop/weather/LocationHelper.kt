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

                // Check if any provider is available
                val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                if (!hasGps && !hasNetwork) {
                    return@withContext Pair(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
                }

                // Try to get last known location first (fast)
                val lastKnown = getLastKnownLocation(locationManager, hasGps, hasNetwork)
                if (lastKnown != null) {
                    return@withContext Pair(lastKnown.latitude, lastKnown.longitude)
                }

                // Request fresh location with timeout
                val freshLocation = requestFreshLocation(locationManager, hasGps, hasNetwork)
                if (freshLocation != null) {
                    return@withContext Pair(freshLocation.latitude, freshLocation.longitude)
                }

                // Fallback
                Pair(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
            } catch (e: Exception) {
                e.printStackTrace()
                Pair(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
            }
        }
    }

    /**
     * Get a human-readable location description from coordinates.
     * Uses Android Geocoder to reverse geocode lat/lon to city name.
     */
    fun getLocationDescription(context: Context, lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.CHINA)
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val city = addr.locality ?: ""
                val district = addr.subLocality ?: ""
                val subAdmin = addr.subAdminArea ?: ""
                val admin = addr.adminArea ?: ""

                // Deduplicate: if district contains city or vice versa, show the more specific one
                when {
                    district.isNotEmpty() && city.isNotEmpty() && district == city -> district
                    district.isNotEmpty() && city.isNotEmpty() && district.contains(city.removeSuffix("市")) -> district
                    city.isNotEmpty() && district.isNotEmpty() && city.contains(district.removeSuffix("区")) -> city
                    district.isNotEmpty() && admin.isNotEmpty() && district == admin -> district
                    district.isNotEmpty() && city.isNotEmpty() && !city.contains("市") -> "$city$district"
                    district.isNotEmpty() && city.isNotEmpty() -> {
                        val cityBase = city.removeSuffix("市")
                        if (district.contains(cityBase)) district else "$city$district"
                    }
                    city.isNotEmpty() -> city
                    district.isNotEmpty() -> district
                    admin.isNotEmpty() -> admin
                    else -> String.format(Locale.US, "%.2f°N, %.2f°E", lat, lon)
                }
            } else {
                String.format(Locale.US, "%.2f°N, %.2f°E", lat, lon)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            String.format(Locale.US, "%.2f°N, %.2f°E", lat, lon)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(
        locationManager: LocationManager,
        hasGps: Boolean,
        hasNetwork: Boolean
    ): Location? {
        try {
            if (hasNetwork) {
                val loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (loc != null) return loc
            }
            if (hasGps) {
                val loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (loc != null) return loc
            }
        } catch (e: SecurityException) {
            // Permission not granted
        }
        return null
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(
        locationManager: LocationManager,
        hasGps: Boolean,
        hasNetwork: Boolean
    ): Location? {
        return withTimeoutOrNull(10_000L) {
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
                    val provider = when {
                        hasNetwork -> LocationManager.NETWORK_PROVIDER
                        hasGps -> LocationManager.GPS_PROVIDER
                        else -> return@suspendCancellableCoroutine
                    }
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
