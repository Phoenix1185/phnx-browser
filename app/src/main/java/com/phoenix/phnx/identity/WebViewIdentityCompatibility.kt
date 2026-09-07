package com.phoenix.phnx.identity

import android.webkit.WebView
import org.json.JSONArray
import org.json.JSONObject

/**
 * Applies only page-visible compatibility values that can be changed without changing Android.
 * Native WebView headers, client hints, display metrics, and input hardware remain untouched.
 */
object WebViewIdentityCompatibility {
    fun install(webView: WebView, profile: DeviceProfile, onComplete: (() -> Unit)? = null) {
        webView.evaluateJavascript(scriptFor(profile)) { onComplete?.invoke() }
    }

    fun scriptFor(profile: DeviceProfile): String {
        val languages = JSONArray().apply { profile.languages.forEach { put(it) } }
        return """
            (function() {
              const target = {
                viewportWidth: ${profile.viewportWidth},
                viewportHeight: ${profile.viewportHeight},
                screenWidth: ${profile.screenWidth},
                screenHeight: ${profile.screenHeight},
                colorDepth: ${profile.colorDepth},
                devicePixelRatio: ${profile.deviceScaleFactor},
                platform: ${JSONObject.quote(profile.platform)},
                language: ${JSONObject.quote(profile.language)},
                languages: $languages,
                timezone: ${JSONObject.quote(profile.timezone)},
                maxTouchPoints: ${if (profile.touchSupport) 5 else 0}
              };
              window.__PHNX_DEVICE_PROFILE = target;

              const define = (object, name, value) => {
                try {
                  Object.defineProperty(object, name, {
                    configurable: true,
                    enumerable: true,
                    get: () => value
                  });
                  return true;
                } catch (_) {
                  try {
                    Object.defineProperty(Object.getPrototypeOf(object), name, {
                      configurable: true,
                      get: () => value
                    });
                    return true;
                  } catch (_) {
                    return false;
                  }
                }
              };

              define(window, "innerWidth", target.viewportWidth);
              define(window, "innerHeight", target.viewportHeight);
              define(window, "devicePixelRatio", target.devicePixelRatio);
              if (window.screen) {
                define(window.screen, "width", target.screenWidth);
                define(window.screen, "height", target.screenHeight);
                define(window.screen, "availWidth", target.screenWidth);
                define(window.screen, "availHeight", target.screenHeight);
                define(window.screen, "colorDepth", target.colorDepth);
                define(window.screen, "pixelDepth", target.colorDepth);
              }
              if (window.navigator) {
                define(window.navigator, "platform", target.platform);
                define(window.navigator, "language", target.language);
                define(window.navigator, "languages", Object.freeze(target.languages.slice()));
                define(window.navigator, "maxTouchPoints", target.maxTouchPoints);
              }

              const resolvedOptions = window.Intl && window.Intl.DateTimeFormat &&
                window.Intl.DateTimeFormat.prototype.resolvedOptions;
              if (resolvedOptions && !resolvedOptions.__phnxWrapped) {
                const wrapped = function() {
                  const options = resolvedOptions.call(this);
                  const active = window.__PHNX_DEVICE_PROFILE;
                  if (active && active.timezone) options.timeZone = active.timezone;
                  return options;
                };
                try {
                  Object.defineProperty(wrapped, "__phnxWrapped", { value: true });
                  window.Intl.DateTimeFormat.prototype.resolvedOptions = wrapped;
                } catch (_) {}
              }
            })();
        """.trimIndent()
    }
}
