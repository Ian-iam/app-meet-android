package kr.app.feeling

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class MainActivity : ComponentActivity(), PermissionManager.PermissionCallback {
    private lateinit var webView: WebView
    private lateinit var webViewLayout: ViewGroup
    private lateinit var customLocationManager: CustomLocationManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var webViewSetup: WebViewSetup

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 스플래시 스크린 설치
        installSplashScreen()

        // 2. Edge-to-Edge 활성화 (시스템 바 투명화)
        enableEdgeToEdge()

        // 3. 기존 View 시스템 사용 (Compose 제거)
        setContentView(R.layout.activity_main)

        // 4. 시스템 바 패딩 자동 적용
        setupSystemBarsPadding()

        // 5. WebView 및 기타 컴포넌트 초기화
        initializeComponents()

        // 6. 권한 요청 및 기타 설정
        setupAppConfiguration()
    }

    /**
     * 시스템 바 패딩 자동 적용 (View 시스템용)
     */
    private fun setupSystemBarsPadding() {
        val rootView = findViewById<ViewGroup>(R.id.webViewLayout)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            // 키보드가 올라왔을 때와 내려갔을 때 처리
            val bottomPadding = if (ime.bottom > systemBars.bottom) {
                ime.bottom // 키보드 높이
            } else {
                systemBars.bottom // 네비게이션 바 높이
            }

            view.updatePadding(
                top = systemBars.top,
                bottom = bottomPadding
            )
            Log.d("Feeling", "키보드 상태 - IME: ${ime.bottom}px, SystemBar: ${systemBars.bottom}px, Applied: ${bottomPadding}px")

            insets
        }
    }

    /**
     * WebView 및 관련 컴포넌트 초기화
     */
    private fun initializeComponents() {
        webView = findViewById(R.id.webView)
        webViewLayout = findViewById(R.id.webViewLayout)

        // UserAgent 수정
        val originalUserAgent = webView.settings.userAgentString
        val newUserAgent = originalUserAgent.replace("; wv)", ")")
        webView.settings.userAgentString = newUserAgent

        // 매니저들 초기화
        customLocationManager = CustomLocationManager(this)
        permissionManager = PermissionManager(this)
        permissionManager.setPermissionCallback(this)

        // WebView 설정
        webViewSetup = WebViewSetup(this, customLocationManager, webViewLayout)
        webViewSetup.setup(webView)

        // 빌링 매니저 초기화
        BillingManager(this, webView)
    }

    /**
     * 앱 기본 설정
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setupAppConfiguration() {
        // 권한 요청
        permissionManager.requestPermissions()

        // 세로 모드 고정
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // 뒤로가기 처리
        setupBackNavigation()
    }

    /**
     * 뒤로가기 내비게이션 설정
     */
    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 웹 페이지의 JavaScript 처리
                webView.evaluateJavascript(
                    "typeof window.handleBackButton === 'function' ? window.handleBackButton() : 'not_defined';"
                ) { result ->
                    when {
                        result.trim('"') == "exit" -> finish()
                        result.trim('"') == "not_defined" -> finish()
                        // 다른 특별한 결과에 대한 처리가 필요하다면 여기에 추가
                    }
                }
            }
        })
    }

    /**
     * 모든 권한이 승인되었을 때 콜백
     */
    override fun onAllPermissionsGranted() {
        customLocationManager.startLocationUpdates()
        Log.d("Feeling", "모든 권한이 승인되어 위치 업데이트를 시작합니다.")
    }

    /**
     * 일부 권한이 거부되었을 때 콜백
     */
    override fun onSomePermissionsDenied(deniedPermissions: Array<String>) {
        Log.d("MainActivity", "일부 권한이 거부되었습니다: ${deniedPermissions.joinToString()}")
    }

    /**
     * 액티비티 종료 시 정리
     */
    override fun onDestroy() {
        webViewSetup.destroyPopupWebViews()
        super.onDestroy()
    }
}