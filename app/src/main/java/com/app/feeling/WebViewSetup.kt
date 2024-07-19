package com.app.feeling

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class WebViewSetup(private val activity: ComponentActivity, private val customLocationManager: CustomLocationManager) {
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var imageChooserLauncher: ActivityResultLauncher<Intent>

    init {
        setupImageChooserLauncher()
    }

    private fun setupImageChooserLauncher() {
        imageChooserLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == ComponentActivity.RESULT_OK) {
                val results = result.data?.data?.let { arrayOf(it) }
                filePathCallback?.onReceiveValue(results)
            } else {
                filePathCallback?.onReceiveValue(null)
            }
            filePathCallback = null
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun setup(webView: WebView) {
        webView.settings.javaScriptEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@WebViewSetup.filePathCallback = filePathCallback
                openImageChooser()
                return true
            }
        }

        webView.addJavascriptInterface(WebAppInterface(activity, customLocationManager), "FeelingAndroidBridge")
        webView.loadUrl("http://192.168.1.4:3000")
    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        imageChooserLauncher.launch(intent)
    }
}