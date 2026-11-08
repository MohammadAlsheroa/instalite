package com.mohammad.instalite

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView

    /** Shortcode of the single reel currently allowed on screen, or null. */
    private var activeReel: String? = null

    /** Page the active reel was opened from (e.g. the DM chat): where we go back to. */
    private var reelOrigin: String? = null

    /** Last non-reel page visited. */
    private var lastNormalUrl: String = HOME

    /** True while jumping back to [reelOrigin]; reel URLs are ignored meanwhile. */
    private var returning = false

    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private val filePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            fileCallback?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            )
            fileCallback = null
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goFullscreen()

        // Debug builds can be inspected from chrome://inspect.
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            // Plain mobile Chrome UA (drop the WebView markers).
            userAgentString = userAgentString
                .replace("; wv", "")
                .replace(Regex("Version/\\d+\\.\\d+ "), "")
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        var script = assets.open("inject.js").bufferedReader().use { it.readText() }
        if (BuildConfig.DMS_ONLY) {
            // Messages-only build: also hide every nav link that leaves the DMs.
            script += "\n" + assets.open("dms.js").bufferedReader().use { it.readText() }
        }
        val docStart = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        if (docStart) {
            WebViewCompat.addDocumentStartJavaScript(webView, script, setOf("https://*.instagram.com"))
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (!isInstagram(uri)) {
                    if (!request.isForMainFrame) return false
                    // Outside links open in the regular browser.
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    return true
                }
                return if (request.isForMainFrame) checkUrl(uri.toString()) else false
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                // Fires for SPA (pushState) navigations too, which is how Instagram moves between reels.
                checkUrl(url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (!docStart) view.evaluateJavascript(script, null)
                CookieManager.getInstance().flush()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                return try {
                    filePicker.launch(params.createIntent()); true
                } catch (e: Exception) {
                    fileCallback = null; false
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })

        if (savedInstanceState != null) webView.restoreState(savedInstanceState)
        else webView.loadUrl(HOME)
    }

    /**
     * Applies the reel rules to [url]. Returns true if the navigation must be blocked.
     *  - The reels feed (/reels/) is never allowed.
     *  - One reel may be opened (e.g. from a DM). Moving to any other reel sends you back
     *    to the page it was opened from.
     */
    private fun checkUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        if (!isInstagram(uri)) return false
        val path = uri.path ?: "/"

        if (BuildConfig.DMS_ONLY && !dmsAllowed(path)) {
            // Messages-only build: anything outside the DMs goes back to the inbox.
            activeReel = null
            reelOrigin = null
            returning = false
            webView.post { webView.loadUrl(HOME) }
            return true
        }

        if (REELS_FEED.matches(path)) {
            returnFromReel()
            return true
        }

        val code = REEL.find(path)?.groupValues?.get(2)
        if (code == null) {
            // Normal page: any reel session is over.
            activeReel = null
            reelOrigin = null
            returning = false
            lastNormalUrl = url
            return false
        }

        if (returning) return true
        if (activeReel == null) {
            activeReel = code
            reelOrigin = lastNormalUrl
            return false
        }
        if (code != activeReel) {
            // Scrolled to another reel.
            returnFromReel()
            return true
        }
        return false
    }

    private fun returnFromReel() {
        if (returning) return
        val target = reelOrigin ?: lastNormalUrl
        returning = true
        webView.post {
            webView.evaluateJavascript(
                "document.querySelectorAll('video,audio').forEach(function(m){m.pause();m.muted=true;});",
                null
            )
            // Jump back through history if the origin is there (no reload), else load it.
            val list = webView.copyBackForwardList()
            var steps = 0
            for (i in list.currentIndex - 1 downTo 0) {
                if (samePath(list.getItemAtIndex(i).url, target)) {
                    steps = i - list.currentIndex
                    break
                }
            }
            if (steps != 0 && webView.canGoBackOrForward(steps)) webView.goBackOrForward(steps)
            else webView.loadUrl(target)
        }
    }

    /** Paths the messages-only build may show: DMs, login/security pages, and content opened from a chat. */
    private fun dmsAllowed(path: String): Boolean =
        DMS_ALLOWED.any { path == it.trimEnd('/') || path.startsWith(it) }

    private fun samePath(a: String, b: String) =
        Uri.parse(a).path?.trimEnd('/') == Uri.parse(b).path?.trimEnd('/')

    private fun isInstagram(uri: Uri): Boolean {
        val host = uri.host ?: return false
        return host == "instagram.com" || host.endsWith(".instagram.com")
    }

    private fun goFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) goFullscreen()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    companion object {
        private val HOME = BuildConfig.HOME_URL
        private val DMS_ALLOWED = listOf(
            "/direct/",                                 // inbox, chats, requests, new message
            "/accounts/", "/challenge/", "/two_factor", // login, 2FA, security checks
            "/reel/", "/p/", "/stories/",               // posts/reels/stories friends send in a chat
        )
        private val REELS_FEED = Regex("^/reels/?$")
        private val REEL = Regex("^/(reel|reels)/([^/]+)")
    }
}
