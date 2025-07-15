package kr.app.feeling

import android.Manifest
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import androidx.core.net.toUri
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL

@Suppress("unused")
class WebAppInterface(
    private val activity: ComponentActivity,
    private val locationManager: CustomLocationManager?,
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
        // 위치 기능 비활성화 - 위치 매니저가 null인 경우 처리
        if (locationManager == null) {
            return "location_disabled"
        }

        // 위치 기능이 필요한 경우 아래 주석을 해제하세요
        /*
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
        */

        // 현재는 위치 기능 비활성화 상태 반환
        return "location_disabled"
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

            /*"location" -> listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )*/

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

    @JavascriptInterface
    fun downloadFile(url: String, fileName: String, isVideo: Boolean) {
        Log.d("WebAppInterface", "다운로드 요청: $url, $fileName, isVideo: $isVideo")

        // 필요한 권한 확인
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (isVideo) {
                listOf(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                listOf(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        activity.runOnUiThread {
            // 권한 확인 후 다운로드 진행
            permissionManager.setPermissionCallback(object : PermissionManager.PermissionCallback {
                override fun onAllPermissionsGranted() {
                    // 권한이 있으면 다운로드 진행
                    executeDownload(url, fileName, isVideo)
                }

                override fun onSomePermissionsDenied(deniedPermissions: Array<String>) {
                    // 권한이 없으면 에러 콜백
                    webView.evaluateJavascript(
                        "window.onDownloadError && window.onDownloadError('다운로드 권한이 필요합니다.')",
                        null
                    )
                }
            })

            // 권한 체크 및 요청
            permissionManager.checkAndRequestPermissions(requiredPermissions, true)
        }
    }

    @JavascriptInterface
    fun checkDownloadPermissions(): String {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val hasPermissions = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermissions) {
            permissionManager.checkAndRequestPermissions(requiredPermissions, true)
        }
        return if (hasPermissions) "granted" else "denied"
    }

    private fun executeDownload(url: String, fileName: String, isVideo: Boolean) {
        try {
            // Android 10+ Scoped Storage 사용
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                downloadWithScopedStorage(url, fileName, isVideo)
            } else {
                downloadWithLegacyMethod(url, fileName, isVideo)
            }
        } catch (e: Exception) {
            Log.e("WebAppInterface", "다운로드 실패", e)
            webView.evaluateJavascript(
                "window.onDownloadError && window.onDownloadError('다운로드에 실패했습니다.')",
                null
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun downloadWithScopedStorage(url: String, fileName: String, isVideo: Boolean) {
        try {
            val mimeType = if (isVideo) "video/mp4" else "image/jpeg"

            // DownloadManager 사용 (Scoped Storage 자동 처리)
            val request = DownloadManager.Request(url.toUri()).apply {
                setMimeType(mimeType)
                setTitle(fileName)
                setDescription("다운로드 중...")

                // Downloads 폴더에 저장 (권한 불필요)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

                // 알림 설정
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                // 네트워크 설정
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            // 다운로드 완료 알림
            webView.evaluateJavascript(
                "window.onDownloadSuccess && window.onDownloadSuccess('$fileName')",
                null
            )

            Log.d("WebAppInterface", "다운로드 시작: $fileName (ID: $downloadId)")

        } catch (e: Exception) {
            Log.e("WebAppInterface", "Scoped Storage 다운로드 실패", e)
            // 대안으로 MediaStore 사용
            downloadWithMediaStore(url, fileName, isVideo)
        }
    }

    private fun downloadWithLegacyMethod(url: String, fileName: String, isVideo: Boolean) {
        try {
            val mimeType = if (isVideo) "video/mp4" else "image/jpeg"

            val request = DownloadManager.Request(url.toUri()).apply {
                setMimeType(mimeType)
                setTitle(fileName)
                setDescription("다운로드 중...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            webView.evaluateJavascript(
                "window.onDownloadSuccess && window.onDownloadSuccess('$fileName')",
                null
            )

        } catch (e: Exception) {
            Log.e("WebAppInterface", "레거시 다운로드 실패", e)
            webView.evaluateJavascript(
                "window.onDownloadError && window.onDownloadError('다운로드에 실패했습니다.')",
                null
            )
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun downloadWithMediaStore(url: String, fileName: String, isVideo: Boolean) {
        // 코루틴을 사용하여 백그라운드에서 다운로드 처리
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val inputStream = URL(url).openStream()
                val mimeType = if (isVideo) "video/mp4" else "image/jpeg"

                val collection = if (isVideo) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                        )
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val uri = activity.contentResolver.insert(collection, contentValues)

                uri?.let { mediaUri ->
                    activity.contentResolver.openOutputStream(mediaUri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        activity.contentResolver.update(mediaUri, contentValues, null, null)
                    }

                    activity.runOnUiThread {
                        webView.evaluateJavascript(
                            "window.onDownloadSuccess && window.onDownloadSuccess('$fileName')",
                            null
                        )
                    }
                }

                inputStream.close()

            } catch (e: Exception) {
                Log.e("WebAppInterface", "MediaStore 다운로드 실패", e)
                activity.runOnUiThread {
                    webView.evaluateJavascript(
                        "window.onDownloadError && window.onDownloadError('다운로드에 실패했습니다.')",
                        null
                    )
                }
            }
        }
    }
}