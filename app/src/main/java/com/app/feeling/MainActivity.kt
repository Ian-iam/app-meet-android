package com.app.feeling

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi

class MainActivity : ComponentActivity(), PermissionManager.PermissionCallback {
    private lateinit var webView: WebView
    private lateinit var customLocationManager: CustomLocationManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var webViewSetup: WebViewSetup

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        customLocationManager = CustomLocationManager(this)
        permissionManager = PermissionManager(this)
        permissionManager.setPermissionCallback(this)
        webViewSetup = WebViewSetup(this, customLocationManager)

        webView = findViewById(R.id.webView)
        webViewSetup.setup(webView)

        permissionManager.requestPermissions()

        setupBackNavigation()
    }

    override fun onAllPermissionsGranted() {
        customLocationManager.startLocationUpdates()
    }

    override fun onSomePermissionsDenied(deniedPermissions: Array<String>) {
        Log.d("MainActivity", "Some permissions were denied: ${deniedPermissions.joinToString()}")
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript("window.handleBackButton && window.handleBackButton();") { result ->
                    when (result.trim('"')) {
                        "exit" -> finish()
                    }
                }
            }
        })
    }
}