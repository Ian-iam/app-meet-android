package com.app.feeling

import android.util.Log
import android.webkit.JavascriptInterface
import androidx.activity.ComponentActivity

@Suppress("unused")
class WebAppInterface(private val activity: ComponentActivity, private val locationManager: CustomLocationManager) {
    @JavascriptInterface
    fun exitApp() {
        activity.runOnUiThread {
            activity.finish()
        }
    }

    @JavascriptInterface
    fun getCurrentLocation(): String {
        val location = locationManager.getCurrentLocationString()
        return when {
            location == "null" -> {
                "Unable to get location"
            }
            location.isEmpty() -> {
                "Unable to get location"
            }
            else -> location
        }
    }

    @JavascriptInterface
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return locationManager.calculateDistance(lat1, lon1, lat2, lon2)
    }
}