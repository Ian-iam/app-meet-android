package com.app.feeling

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: ComponentActivity) {
    private var permissionCallback: PermissionCallback? = null

    interface PermissionCallback {
        fun onAllPermissionsGranted()
        fun onSomePermissionsDenied(deniedPermissions: Array<String>)
    }

    fun setPermissionCallback(callback: PermissionCallback) {
        this.permissionCallback = callback
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.INTERNET,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        // Android 13 (API 33) 이상에서만 알림 권한 추가
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionRequest.launch(missingPermissions.toTypedArray())
        } else {
            permissionCallback?.onAllPermissionsGranted()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val permissionRequest = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            Log.d("Permission", "${it.key} is ${if (it.value) "granted" else "denied"}")
        }

        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            permissionCallback?.onAllPermissionsGranted()
        } else {
            val deniedPermissions = permissions.filter { !it.value }.keys.toTypedArray()
            showFeatureLimitedDialog()
            permissionCallback?.onSomePermissionsDenied(deniedPermissions)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun showFeatureLimitedDialog() {
        val permanentlyDeniedPermissions = getPermanentlyDeniedPermissions()
        val permissionDetails = getPermissionDetails(permanentlyDeniedPermissions)

        val message = buildString {
            append("다음 권한이 영구적으로 거부되어 앱의 일부 기능이 제한됩니다:\n\n")
            append(permissionDetails)
            append("\n\n권한을 부여하려면 다음 단계를 따르세요:\n\n")
            append("1. 휴대폰의 '설정' 앱을 엽니다.\n")
            append("2. '앱' 또는 '애플리케이션 관리'를 찾아 탭합니다.\n")
            append("3. 앱 목록에서 '${activity.applicationInfo.loadLabel(activity.packageManager)}'을(를) 찾아 탭합니다.\n")
            append("4. '권한'을 탭합니다.\n")
            append("5. 나열된 각 권한을 켜기로 설정합니다.\n\n")
            append("권한 설정 후 앱을 다시 실행해 주세요.")
        }

        AlertDialog.Builder(activity)
            .setTitle("기능 제한")
            .setMessage(message)
            .setNeutralButton("창 닫기") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("앱 종료") { _, _ ->
                activity.finish()
            }
            .setPositiveButton("설정으로 이동") { _, _ ->
                openAppSettings()
            }
            .setCancelable(false)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getPermanentlyDeniedPermissions(): List<String> {
        return listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_MEDIA_IMAGES
        ).filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
                    && !activity.shouldShowRequestPermissionRationale(it)
        }
    }

    private fun getPermissionDetails(permissions: List<String>): String {
        return permissions.joinToString("\n") { permission ->
            when (permission) {
                Manifest.permission.ACCESS_FINE_LOCATION ->
                    "- 정확한 위치: 정밀한 위치 기반 기능 사용 (예: 내비게이션, 주변 장소 검색)"

                Manifest.permission.ACCESS_COARSE_LOCATION ->
                    "- 대략적 위치: 광역 위치 기반 서비스 이용 (예: 날씨 정보, 지역 뉴스)"

                Manifest.permission.CAMERA -> "- 카메라: 사진 및 비디오 촬영"
                Manifest.permission.POST_NOTIFICATIONS -> "- 알림: 중요 업데이트 및 정보 전송"
                Manifest.permission.READ_MEDIA_IMAGES -> "- 사진 접근: 기기의 사진 및 이미지 보기 및 선택 (예: 프로필 사진 업로드, 이미지 공유)"
                else -> "- ${permission.split(".").last()}: 관련 기능"
            }
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivity(intent)
    }
}