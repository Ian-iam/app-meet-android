package com.app.feeling

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: ComponentActivity) {
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
            if (deniedPermissions.any { activity.shouldShowRequestPermissionRationale(it) }) {
                showPermissionRationaleDialog(deniedPermissions)
            } else {
                showFeatureLimitedDialog()
            }
            permissionCallback?.onSomePermissionsDenied(deniedPermissions)
        }
    }

    private var permissionCallback: PermissionCallback? = null

    interface PermissionCallback {
        fun onAllPermissionsGranted()
        fun onSomePermissionsDenied(deniedPermissions: Array<String>)
    }

    fun setPermissionCallback(callback: PermissionCallback) {
        this.permissionCallback = callback
    }

    fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            if (missingPermissions.any { activity.shouldShowRequestPermissionRationale(it) }) {
                showPermissionRationaleDialog(missingPermissions.toTypedArray())
            } else {
                permissionRequest.launch(permissions)
            }
        } else {
            permissionCallback?.onAllPermissionsGranted()
        }
    }

    private fun showPermissionRationaleDialog(permissions: Array<String>) {
        val message = buildString {
            append("This app requires the following permissions to function properly:\n\n")
            if (permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION) ||
                permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION)
            ) {
                append("- Location: To provide location-based services\n")
            }
            if (permissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                permissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ) {
                append("- Storage: To access and save files on your device\n")
            }
            if (permissions.contains(Manifest.permission.CAMERA)) {
                append("- Camera: To take photos or videos\n")
            }
            append("\nPlease grant these permissions in the next screen.")
        }

        AlertDialog.Builder(activity)  // activity를 Context로 사용
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                permissionRequest.launch(permissions)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                // 사용자에게 권한이 없어 일부 기능을 사용할 수 없음을 알림
                showFeatureLimitedDialog()
            }
            .show()
    }

    private fun showFeatureLimitedDialog() {
        AlertDialog.Builder(activity)  // activity를 Context로 사용
            .setTitle("Feature Limited")
            .setMessage("Some features of the app will be limited due to missing permissions. You can grant these permissions later in the app settings if you change your mind.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}