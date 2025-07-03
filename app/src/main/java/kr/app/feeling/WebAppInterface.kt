package kr.app.feeling

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.ComponentActivity
import com.google.firebase.messaging.FirebaseMessaging

@Suppress("unused")
class WebAppInterface(
    private val activity: ComponentActivity,
    private val locationManager: CustomLocationManager,
    private val billingManager: BillingManager,
    private val webView: WebView,
    private val onTokenReceived: (String) -> Unit,
    private val permissionManager: PermissionManager
) {
    @JavascriptInterface
    fun exitApp() {
        activity.runOnUiThread {
            activity.finish()
        }
    }

    @JavascriptInterface
    fun getCurrentLocation(): String {
        if (!locationManager.isLocationEnabled()) {
            activity.runOnUiThread {
                AlertDialog.Builder(activity)
                    .setTitle("위치 서비스 꺼짐")
                    .setMessage("위치 서비스가 비활성화되어 있습니다. 설정에서 위치 서비스를 켜주세요.")
                    .setPositiveButton("설정") { _, _ ->
                        activity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
            return "location_disabled"
        }


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
    fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                activity.runOnUiThread {
                    onTokenReceived(token)
                }
            } else {
                activity.runOnUiThread {
                    onTokenReceived("") // 에러 시 빈 문자열 전달
                }
            }
        }
    }

    @JavascriptInterface
    fun getDeviceType(): String {
        return "ANDROID"
    }

    @JavascriptInterface
    fun getLastNotificationType(): String {
        val sharedPref = activity.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
        val type = sharedPref.getString("last_notification_type", "") ?: ""

        // 값을 읽은 후 즉시 삭제
        if (type.isNotEmpty()) {
            with(sharedPref.edit()) {
                remove("last_notification_type")
                apply()
            }
        }
        return type
    }

    @JavascriptInterface
    fun purchaseItem(productId: String) {
        activity.runOnUiThread {
            try {
                billingManager.queryProductDetails(productId)
            } catch (e: Exception) {
                // 실패 시 JavaScript 콜백 호출
                webView.evaluateJavascript("window.onPurchaseFailed();", null)
            }
        }
    }

    @JavascriptInterface
    fun getProductDetails() {
        activity.runOnUiThread {
            billingManager.queryProductList()
        }
    }

    @JavascriptInterface
    fun checkAndRequestPermissions(type: String, showDialog: Boolean = true) {
        val requiredPermissions = when (type) {
            "photo" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            "location" -> listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )

            else -> emptyList()
        }

        if (requiredPermissions.isEmpty()) {
            webView.evaluateJavascript("onPermissionCheck(false)", null)
            return
        }

        activity.runOnUiThread {
            permissionManager.setPermissionCallback(object : PermissionManager.PermissionCallback {
                override fun onAllPermissionsGranted() {
                    webView.evaluateJavascript("onPermissionCheck(true)", null)
                }

                override fun onSomePermissionsDenied(deniedPermissions: Array<String>) {
                    webView.evaluateJavascript("onPermissionCheck(false)", null)
                }
            })
            permissionManager.checkAndRequestPermissions(requiredPermissions, showDialog)
        }
    }
}