package com.phoenix.phnx.identity

import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.PhnxApplication
import com.phoenix.phnx.browser.BrowserView

class IdentityDiagnosticActivity : AppCompatActivity() {
    private val app by lazy { application as PhnxApplication }
    private val profileId by lazy {
        intent.getStringExtra(EXTRA_PROFILE_ID) ?: app.profileManager.activeProfile().id
    }
    private lateinit var diagnosticWebView: BrowserView
    private var pageReady = false
    private val readyCallbacks = mutableListOf<(WebView) -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Runtime diagnostics"

        val profile = app.deviceProfileManager.getProfileConfiguration(profileId)
        diagnosticWebView = BrowserView(this, isPrivateTab = false)
        WebViewIdentityAdapter().apply(diagnosticWebView, profile)
        diagnosticWebView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                WebViewIdentityCompatibility.install(view, profile)
            }

            override fun onPageFinished(view: WebView, url: String) {
                WebViewIdentityCompatibility.install(view, profile) {
                    view.evaluateJavascript("window.__PHNX_RENDER_DIAGNOSTICS && window.__PHNX_RENDER_DIAGNOSTICS()") {
                        pageReady = true
                        readyCallbacks.drain(view)
                    }
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
        }
        setContentView(diagnosticWebView)
        diagnosticWebView.loadDataWithBaseURL(
            "https://phnx.local/diagnostics",
            diagnosticHtml(profile),
            "text/html",
            "UTF-8",
            null,
        )
    }

    /** Used by the instrumentation test to read the same values shown on screen. */
    fun whenDiagnosticsReady(callback: (WebView) -> Unit) {
        if (pageReady) callback(diagnosticWebView) else readyCallbacks += callback
    }

    override fun onDestroy() {
        readyCallbacks.clear()
        if (::diagnosticWebView.isInitialized) diagnosticWebView.destroy()
        super.onDestroy()
    }

    private fun diagnosticHtml(profile: DeviceProfile): String = """
        <!doctype html>
        <html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <style>
          body { margin: 0; padding: 24px; background: #f7f1e5; color: #17253f; font-family: sans-serif; }
          h1 { color: #164b8a; margin-top: 0; }
          p { line-height: 1.5; }
          table { width: 100%; border-collapse: collapse; background: #fff; }
          td { border-bottom: 1px solid #d9dfeb; padding: 10px; vertical-align: top; word-break: break-word; }
          td:first-child { width: 34%; font-weight: 700; }
          .note { color: #59657a; }
        </style></head>
        <body>
          <h1>PHNX runtime diagnostics</h1>
          <p>Observed values from this WebView after applying <strong>${escapeHtml(profile.name)}</strong>.</p>
          <p class="note">Native WebView values, JavaScript compatibility values, and WebView-controlled values are intentionally shown as observed. This page does not change the Android display.</p>
          <table><tbody id="values"></tbody></table>
          <script>
            (function() {
              const escape = value => String(value == null ? "unavailable" : value)
                .replaceAll("&", "&amp;").replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;").replaceAll('"', "&quot;");
              window.__PHNX_RENDER_DIAGNOSTICS = function() {
                const uaData = navigator.userAgentData;
                const webgl = (() => {
                  try {
                    const canvas = document.createElement("canvas");
                    const gl = canvas.getContext("webgl") || canvas.getContext("experimental-webgl");
                    if (!gl) return "unavailable";
                    const debug = gl.getExtension("WEBGL_debug_renderer_info");
                    return debug ? JSON.stringify({
                      vendor: gl.getParameter(debug.UNMASKED_VENDOR_WEBGL),
                      renderer: gl.getParameter(debug.UNMASKED_RENDERER_WEBGL)
                    }) : "available; unmasked renderer unavailable";
                  } catch (_) {
                    return "unavailable";
                  }
                })();
                const rows = [
                  ["navigator.userAgent", navigator.userAgent],
                  ["navigator.platform", navigator.platform],
                  ["navigator.vendor", navigator.vendor],
                  ["navigator.language", navigator.language],
                  ["navigator.languages", JSON.stringify(navigator.languages)],
                  ["navigator.maxTouchPoints", navigator.maxTouchPoints],
                  ["navigator.hardwareConcurrency", navigator.hardwareConcurrency],
                  ["navigator.deviceMemory", navigator.deviceMemory],
                  ["window.innerWidth", window.innerWidth],
                  ["window.innerHeight", window.innerHeight],
                  ["window.outerWidth", window.outerWidth],
                  ["window.outerHeight", window.outerHeight],
                  ["visualViewport", window.visualViewport ? JSON.stringify({width: window.visualViewport.width, height: window.visualViewport.height, scale: window.visualViewport.scale}) : "unavailable"],
                  ["screen.width", screen.width],
                  ["screen.height", screen.height],
                  ["screen.availWidth", screen.availWidth],
                  ["screen.availHeight", screen.availHeight],
                  ["screen.colorDepth", screen.colorDepth],
                  ["devicePixelRatio", window.devicePixelRatio],
                  ["screen.orientation", screen.orientation ? screen.orientation.type : "unavailable"],
                  ["pointer: coarse", window.matchMedia ? window.matchMedia("(pointer: coarse)").matches : "unavailable"],
                  ["touch event surface", "ontouchstart" in window],
                  ["timezone", Intl.DateTimeFormat().resolvedOptions().timeZone],
                  ["navigator.userAgentData.brands", uaData ? JSON.stringify(uaData.brands) : "unavailable"],
                  ["navigator.userAgentData.mobile", uaData ? uaData.mobile : "unavailable"],
                  ["navigator.userAgentData.platform", uaData ? uaData.platform : "unavailable"],
                  ["WebGL vendor and renderer", webgl],
                  ["Canvas 2D", document.createElement("canvas").getContext("2d") ? "available" : "unavailable"],
                  ["AudioContext", window.AudioContext || window.webkitAudioContext ? "available" : "unavailable"]
                ];
                document.getElementById("values").innerHTML = rows
                  .map(row => "<tr><td>" + escape(row[0]) + "</td><td>" + escape(row[1]) + "</td></tr>")
                  .join("");
              };
              window.__PHNX_RENDER_DIAGNOSTICS();
            })();
          </script>
        </body></html>
    """.trimIndent()

    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun MutableList<(WebView) -> Unit>.drain(view: WebView) {
        toList().forEach { it(view) }
        clear()
    }

    companion object {
        const val EXTRA_PROFILE_ID = "profile_id"
    }
}
