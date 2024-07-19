package com.app.feeling

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

class MainActivity : ComponentActivity(), PermissionManager.PermissionCallback {
    private lateinit var webView: WebView
    private lateinit var customLocationManager: CustomLocationManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var webViewSetup: WebViewSetup

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
        // Handle denied permissions
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