package dev.omalauncher

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.omalauncher.ui.Mono
import dev.omalauncher.ui.Palette
import kotlin.math.roundToInt

/**
 * A page for working on, apart from home: the launcher's development browser.
 *
 * It is not the widgets' `Browser` node and shares nothing with it but the WebView's own hardening. Where a tile's
 * page is https only, with the host written in the widget file and no way to steer it from outside, this one takes
 * any URL the agent hands it, `http://` included but only on this phone's loopback, so a site served from the Arch
 * guest can be looked at while it is being made, and attached to with the Chrome DevTools Protocol.
 *
 * It runs in its own process with its own WebView data directory ([WebView.setDataDirectorySuffix]), so a sign-in
 * here is not a sign-in in a tile, and the debug socket exists only in that process: it goes away when this window
 * does. Neither the tile's page nor the launcher's cookies are reachable from it.
 *
 * Opened from the guest's `browser-dev` command (see OpenBridge), which takes a URL or a port.
 */
class BrowserDevActivity : InstantActivity() {
    /** The URL last asked for, so `browser-dev` run twice reloads this window instead of opening another. */
    private var requested by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before any WebView in this process: its own data directory, since the launcher's is in use by home.
        WebView.setDataDirectorySuffix("dev")
        // The whole point: an agent attaches with the Chrome DevTools Protocol. Process-wide, so this process only.
        WebView.setWebContentsDebuggingEnabled(true)
        super.onCreate(savedInstanceState)
        requested = intent?.getStringExtra(EXTRA_URL)
        setInstantContent { BrowserDevPage(requested, onClose = { closeInstantly() }) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_URL)?.let { requested = it }
    }

    /** The launcher's own way in: `openDev(context, "5173")` and the guest's `browser-dev`. */
    companion object {
        const val EXTRA_URL = "url"

        /** With nothing named, a port every dev server on this phone starts at. */
        const val DEFAULT = "5173"

        fun open(
            context: Context,
            url: String,
        ) {
            context.startActivity(
                Intent(context, BrowserDevActivity::class.java)
                    .putExtra(EXTRA_URL, url)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }

        /**
         * What a typed or written URL means: a port or a bare loopback host becomes `http://`, anything else
         * `https://`; null when the scheme or the host is one this window won't load.
         */
        fun normalize(input: String): String? {
            val text = input.trim().ifEmpty { return null }
            val url =
                when {
                    text.all { it.isDigit() } -> "http://127.0.0.1:$text/"
                    "://" in text -> text
                    loopbackHost(text.substringBefore('/').substringBefore(':')) -> "http://$text"
                    else -> "https://$text"
                }
            val parsed = Uri.parse(url)
            return when (parsed.scheme?.lowercase()) {
                "https" -> if (parsed.host.isNullOrEmpty()) null else url
                // In the clear, but it never leaves the phone: the guest shares its loopback.
                "http" -> if (loopbackHost(parsed.host.orEmpty())) url else null
                else -> null
            }
        }

        private fun loopbackHost(host: String): Boolean =
            when (host.lowercase().trim('[', ']')) {
                "localhost", "127.0.0.1", "::1" -> true
                else -> host.startsWith("127.")
            }
    }
}

@Composable
private fun BrowserDevPage(
    requested: String?,
    onClose: () -> Unit,
) {
    var address by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var note by remember { mutableStateOf("") }
    var view by remember { mutableStateOf<WebView?>(null) }

    fun go(input: String) {
        val url = BrowserDevActivity.normalize(input)
        if (url == null) {
            note = "Only https, or http on this phone (localhost, 127.0.0.1, ::1, or a port)."
            return
        }
        note = ""
        address = url
        view?.loadUrl(url)
    }

    LaunchedEffect(requested) { go(requested ?: BrowserDevActivity.DEFAULT) }

    BackHandler {
        val v = view
        if (v != null && v.canGoBack()) v.goBack() else onClose()
    }

    Column(Modifier.fillMaxSize().background(Palette.background).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Round("‹") { view?.let { if (it.canGoBack()) it.goBack() } }
            Round("›") { view?.let { if (it.canGoForward()) it.goForward() } }
            Round("⟳") { view?.reload() }
            BasicTextField(
                value = address,
                onValueChange = { address = it },
                singleLine = true,
                textStyle = Mono.copy(color = Palette.foreground),
                cursorBrush = SolidColor(Palette.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { go(address) }),
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .background(Palette.surface, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Round("✕") { onClose() }
        }

        // What the page is, and how far it has got.
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                title.ifEmpty { "no title" },
                style = Mono.copy(color = Palette.muted),
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            if (progress > 0f && progress < 1f) {
                BasicText("${(progress * 100).roundToInt()}%", style = Mono.copy(color = Palette.muted))
            }
        }

        AndroidView(
            factory = { context ->
                WebView(context).also { v ->
                    harden(v)
                    v.webViewClient =
                        object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                v: WebView,
                                r: WebResourceRequest,
                            ): Boolean {
                                val scheme = r.url.scheme.orEmpty().lowercase()
                                if (scheme == "https") return false
                                if (scheme == "http") {
                                    // In the clear, but only ever to this phone's own loopback.
                                    if (BrowserDevActivity.normalize(r.url.toString()) != null) return false
                                    note = "That link is http somewhere else, which this window won't load."
                                    return true
                                }
                                if (scheme in BLOCKED) return true
                                v.context.openUrl(r.url.toString())
                                return true
                            }

                            override fun onPageStarted(
                                v: WebView,
                                url: String?,
                                favicon: Bitmap?,
                            ) {
                                url?.let { address = it }
                                progress = 0f
                            }

                            override fun doUpdateVisitedHistory(
                                v: WebView,
                                url: String?,
                                isReload: Boolean,
                            ) {
                                url?.let { address = it }
                            }
                        }
                    v.webChromeClient =
                        object : WebChromeClient() {
                            override fun onReceivedTitle(
                                v: WebView,
                                t: String?,
                            ) {
                                t?.takeIf { it.isNotBlank() }?.let { title = it }
                            }

                            override fun onProgressChanged(
                                v: WebView,
                                newProgress: Int,
                            ) {
                                progress = (newProgress / 100f).coerceIn(0f, 1f)
                            }
                        }
                    view = v
                }
            },
            update = { /* the load is driven by go(), not by recomposition */ },
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 6.dp),
        )

        // The way in for an agent: the socket is this process's, so the pid is the dev process's own.
        BasicText(
            note.ifEmpty {
                "adb forward tcp:9222 localabstract:webview_devtools_remote_${android.os.Process.myPid()}" +
                    "   ·   only https, or http on this phone"
            },
            style = Mono.copy(color = if (note.isEmpty()) Palette.muted else Palette.red),
            maxLines = 2,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun Round(
    label: String,
    onTap: () -> Unit,
) {
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .background(Palette.surface, CircleShape)
            .border(1.dp, Palette.border, CircleShape)
            .clickable { onTap() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = Mono.copy(color = Palette.foreground))
    }
}

/** The WebView's own rules, the ones a tile's page gets too, minus the ones a dev page needs. */
private fun harden(v: WebView) {
    v.setBackgroundColor(Palette.background.toArgb())
    val s = v.settings
    // A dev page is usually a script app, so script and DOM storage are on here where a tile needs js: true.
    s.setJavaScriptEnabled(true)
    s.setDomStorageEnabled(true)
    // Still no reach into the phone's files or a content:// provider.
    s.setAllowFileAccess(false)
    s.setAllowContentAccess(false)
    s.setGeolocationEnabled(false)
    s.setSupportMultipleWindows(false)
    s.setMediaPlaybackRequiresUserGesture(true)
    s.setSupportZoom(true)
    s.setBuiltInZoomControls(true)
    s.setDisplayZoomControls(false)
    s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW)
    CookieManager.getInstance().setAcceptThirdPartyCookies(v, false)
}

/** Schemes that load nothing here: only https, and http on loopback, become pages. */
private val BLOCKED = setOf("file", "content", "javascript", "data")
