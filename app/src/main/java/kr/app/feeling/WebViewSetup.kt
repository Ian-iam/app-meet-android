package kr.app.feeling

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import java.net.URISyntaxException

class WebViewSetup(
    private val activity: ComponentActivity,
    private val customLocationManager: CustomLocationManager?, // nullable로 변경
    private val webViewLayout: ViewGroup
) {
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var imageChooserLauncher: ActivityResultLauncher<Intent>
    private lateinit var billingManager: BillingManager
    private val popupWebViews = mutableListOf<WebView>()

    init {
        setupImageChooserLauncher()
    }

    private fun setupImageChooserLauncher() {
        imageChooserLauncher =
            activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
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
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.setSupportMultipleWindows(true)

        webView.webViewClient = object: WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView,request: WebResourceRequest): Boolean {
                val url = request.url.toString()

                if (request.url.scheme == "intent") {
                    try {
                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)

                        if (intent.resolveActivity(activity.packageManager) != null) {
                            activity.startActivity(intent)
                            return true
                        }

                        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                        if (fallbackUrl != null) {
                            view.loadUrl(fallbackUrl)
                            return true
                        }

                    } catch (e: URISyntaxException) {
                        Log.e("WebViewSetup", "Invalid intent request", e)
                    }
                }
                if (url.startsWith("kakaolink:") ||
                    url.startsWith("kakaotalk:")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        activity.startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        Log.e("WebViewSetup", "Failed to launch app", e)
                    }
                }
                return false
            }
        }

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

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                val messageLevel = consoleMessage.messageLevel()
                val message = consoleMessage.message()
                val sourceId = consoleMessage.sourceId()
                val lineNumber = consoleMessage.lineNumber()

                if (messageLevel == ConsoleMessage.MessageLevel.ERROR) {
                    activity.runOnUiThread {
                        webView.evaluateJavascript(
                            "window.appLog?.(`Android WebView Error: $message at line $lineNumber in ${sourceId}`)",
                            null
                        )
                    }
                } else {
                    Log.d("WebView", "$message -- From line $lineNumber of $sourceId")
                }

                return true
            }

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                Log.d("WebViewSetup", "onCreateWindow called!")
                val childWebView = WebView(view.context).apply {
                    settings.run {
                        javaScriptEnabled = true
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(true)
                        domStorageEnabled = true  // localStorage 지원 추가
                        allowFileAccess = true    // 기존 설정과 동일하게 유지
                        allowContentAccess = true // 기존 설정과 동일하게 유지
                    }

                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    webViewClient = view.webViewClient
                }
                popupWebViews.add(childWebView)
                activity.runOnUiThread {
                    webViewLayout.addView(childWebView)
                }

                val transport = resultMsg.obj as WebView.WebViewTransport
                transport.webView = childWebView
                resultMsg.sendToTarget()
                return true
            }
            override fun onCloseWindow(window: WebView) {
                removePopupWebView(window)
            }
        }
        billingManager = BillingManager(activity, webView)
        val permissionManager = PermissionManager(activity)

        // WebAppInterface 생성 시 위치 매니저를 nullable로 처리
        val webAppInterface = WebAppInterface(
            activity,
            customLocationManager, // nullable 전달
            billingManager,
            webView,
            { token ->
                webView.evaluateJavascript("window.onFCMTokenReceived('$token');", null)
            },
            permissionManager
        )
        webView.addJavascriptInterface(webAppInterface, "FeelingAndroidBridge")
        Log.d("WebView", "Bridge methods: ${webAppInterface.javaClass.methods.map { it.name }}")

        webView.loadUrl(BuildConfig.WEB_VIEW_URL)

    }
    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        imageChooserLauncher.launch(intent)
    }
    private fun removePopupWebView(webView: WebView) {
        activity.runOnUiThread {
            webViewLayout.removeView(webView)
            popupWebViews.remove(webView)
            webView.destroy()
        }
    }
    fun destroyPopupWebViews() {
        popupWebViews.forEach { webView ->
            webViewLayout.removeView(webView)
            webView.destroy()
        }
        popupWebViews.clear()
    }
}