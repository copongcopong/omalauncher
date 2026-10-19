package dev.omalauncher.ui

import android.content.Context
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import dev.omalauncher.BuildConfig
import dev.omalauncher.openUrl
import kotlin.math.roundToInt

/**
 * Web pages in home-screen tiles (the widgets' `Browser` node), drawn by
 * Android's own WebView inside the tile: no guest, no window manager, no
 * Linux. Each tile keeps its view while its page is off screen, so switching
 * tabs comes back to the page where it was; the widget's removal, or a change
 * to its url, ends it.
 *
 * What a page may reach is decided here and nowhere else: script and DOM
 * storage only with `js: true`, no file or content access, no geolocation, no
 * popups, no media that plays on its own, and nothing but https in the page
 * itself. Main thread only.
 */
class EmbeddedBrowser internal constructor(
    /** The https URL the node asked for; a tile whose url changes gets a new handle. */
    val url: String?,
    /** Whether the page may run script; with it, DOM storage too. */
    val js: Boolean,
    /** The page's text size as a multiple of the page's own. */
    val zoom: Double,
) {
    /** The page's title as it loads; a blank one is ignored, so the last real title stays. */
    var title by mutableStateOf<String?>(null)
        private set

    /** How much of the page has loaded, 0 to 1. */
    var progress by mutableStateOf(0f)
        private set

    /** The URL showing now, which follows a link the page opened. */
    var currentUrl by mutableStateOf(url.orEmpty())
        private set

    /** Told when the page's title changes; the widget's onTitle. */
    var onTitle: ((String) -> Unit)? = null

    /** Told when the page goes somewhere else; the widget's onNavigate. */
    var onNavigate: ((String) -> Unit)? = null

    /** Told how much has loaded; the widget's onProgress. */
    var onProgress: ((Double) -> Unit)? = null

    /** The view, kept while the tile is off screen; null until the tile first shows or once closed. */
    private var view: WebView? = null

    private var closed = false

    /**
     * The WebView this tile draws in: the one this handle already has (a tab switched back to),
     * or a new one set up for this node.
     */
    fun attach(context: Context): WebView {
        view?.let { kept ->
            if (kept.isAttachedToWindow) return kept
            // Back on screen: a paused page's timers run again.
            kept.onResume()
            return kept
        }
        val v = WebView(context)
        configure(v)
        v.webViewClient = Client()
        v.webChromeClient = Chrome()
        view = v
        if (!closed) load()
        return v
    }

    /** The tile has gone off screen. The view isn't destroyed: it keeps its page, and its timers wait. */
    fun detach(view: WebView) {
        if (this.view !== view) return
        view.onPause()
    }

    /** Loads [link], or the node's own url with none; what [attach] does the first time. */
    fun load(link: String = url.orEmpty()) {
        if (link.startsWith("https://")) view?.loadUrl(link)
    }

    fun back() {
        view?.let { if (it.canGoBack()) it.goBack() }
    }

    fun forward() {
        view?.let { if (it.canGoForward()) it.goForward() }
    }

    fun reload() {
        view?.reload()
    }

    fun stop() {
        view?.stopLoading()
    }

    /** Ends the page (the widget was removed, or its url changed); a tab switch doesn't. */
    fun close() {
        closed = true
        view?.let {
            it.stopLoading()
            it.destroy()
        }
        view = null
    }

    private fun configure(v: WebView) {
        // WebView's own debugging port is for debug builds only.
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        v.setBackgroundColor(Palette.background.toArgb())
        val s = v.settings
        // Storage and script go together: a page without script has nothing to keep in DOM storage.
        s.setJavaScriptEnabled(js)
        s.setDomStorageEnabled(js)
        // A tile shows a page, not the phone's files: no file:// and nothing a content:// provider holds.
        s.setAllowFileAccess(false)
        s.setAllowContentAccess(false)
        // A page never asks the phone where it is; this is the setting beside the permission a tile mustn't want.
        s.setGeolocationEnabled(false)
        // target="_blank" doesn't open a second window the tile has nowhere to draw.
        s.setSupportMultipleWindows(false)
        // A page's sound waits for a tap, as everything else on home does.
        s.setMediaPlaybackRequiresUserGesture(true)
        // Pinch to zoom; the on-screen zoom buttons would cover the page.
        s.setSupportZoom(true)
        s.setBuiltInZoomControls(true)
        s.setDisplayZoomControls(false)
        s.setTextZoom((zoom * 100).roundToInt().coerceIn(MIN_ZOOM_PERCENT, MAX_ZOOM_PERCENT))
        // An https page can't pull in http: that would be in the clear, and looks no different.
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW)
        // Every Browser tile shares this profile, so a sign-in in one is a sign-in in the others. A third
        // party's cookie is a tracker with nowhere useful to go.
        CookieManager.getInstance().setAcceptThirdPartyCookies(v, false)
    }

    private inner class Client : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean {
            val link = request.url
            val scheme = link.scheme.orEmpty().lowercase()
            // The page itself loads over https only.
            if (scheme == "https") return false
            // Nothing loads over these: http would be in the clear, and the rest reach the phone's files
            // or run script from the URL, which a tile's page has no business doing.
            if (scheme in BLOCKED) return true
            // A link another app owns (mailto:, tel:, market:) goes there, as the url(...) action does.
            view.context.openUrl(link.toString())
            return true
        }

        override fun onPageStarted(
            view: WebView,
            url: String?,
            favicon: Bitmap?,
        ) {
            show(url.orEmpty())
        }

        override fun doUpdateVisitedHistory(
            view: WebView,
            url: String?,
            isReload: Boolean,
        ) {
            show(url.orEmpty())
        }
    }

    private inner class Chrome : WebChromeClient() {
        override fun onReceivedTitle(
            view: WebView,
            title: String?,
        ) {
            val t = title?.takeIf { it.isNotBlank() } ?: return
            this@EmbeddedBrowser.title = t
            onTitle?.invoke(t)
        }

        override fun onProgressChanged(
            view: WebView,
            newProgress: Int,
        ) {
            val p = (newProgress / 100f).coerceIn(0f, 1f)
            progress = p
            onProgress?.invoke(p.toDouble())
        }
    }

    /** The page is now at [url]; tells onNavigate once per change. */
    private fun show(url: String) {
        if (url.isEmpty() || url == currentUrl) return
        currentUrl = url
        onNavigate?.invoke(url)
    }

    private companion object {
        /** Schemes a tile's page may not load: another is a link, https or not at all. */
        val BLOCKED = setOf("http", "file", "content", "javascript", "data")

        const val MIN_ZOOM_PERCENT = 50
        const val MAX_ZOOM_PERCENT = 300
    }
}

/** The tile's content: the page, filling the tile. Its handlers are the node's, run as the page changes. */
@Composable
fun BrowserView(
    browser: EmbeddedBrowser,
    modifier: Modifier,
    onTitle: ((String) -> Unit)? = null,
    onNavigate: ((String) -> Unit)? = null,
    onProgress: ((Double) -> Unit)? = null,
) {
    // The widget is rebuilt as its values change: the page's handlers follow it rather than the first ones.
    SideEffect {
        browser.onTitle = onTitle
        browser.onNavigate = onNavigate
        browser.onProgress = onProgress
    }
    val background = Palette.background
    AndroidView(
        factory = { browser.attach(it) },
        update = { it.setBackgroundColor(background.toArgb()) },
        onRelease = { browser.detach(it) },
        modifier = modifier,
    )
}
