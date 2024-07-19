package com.app.feeling

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class CustomLocationManager(context: Context) {
    private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var currentLocation: Location? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (!isLocationEnabled()) {
            return
        }

        val locationListener = LocationListener { location ->
            currentLocation = location
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000L,
                10f,
                locationListener
            )
        } catch (e: Exception) {
            Log.e("Location", "Error requesting location updates from GPS_PROVIDER", e)
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000L,
                10f,
                locationListener
            )
        } catch (e: Exception) {
            Log.e("Location", "Error requesting location updates from NETWORK_PROVIDER", e)
        }

        // 마지막 알려진 위치 가져오기
        currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (currentLocation == null) {
            currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }
    }

    private fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun getCurrentLocationString(): String {
        return currentLocation?.let { "${it.latitude},${it.longitude}" } ?: run {
            Log.d("CustomLocationManager", "getCurrentLocationString: currentLocation is null")
            "null"
        }
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthR = 6371 // 지구의 반경 (km)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthR * c
    }
}