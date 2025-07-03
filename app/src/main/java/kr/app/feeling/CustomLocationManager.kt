package kr.app.feeling

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import android.Manifest

class CustomLocationManager(private val context: Context) {
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

    fun isLocationEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun getCurrentLocationString(): String {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (currentLocation == null) {
                    currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }
                return currentLocation?.let { "${it.latitude},${it.longitude}" } ?: "null"
            }
            return "null"
        } catch (e: SecurityException) {
            Log.e("Location", "Security exception when getting location", e)
            return "null"
        }
    }
}