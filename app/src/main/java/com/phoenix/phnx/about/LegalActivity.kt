package com.phoenix.phnx.about

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.phoenix.phnx.R

class LegalActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PAGE = "legal_page"
        const val PRIVACY = "privacy"
        const val TERMS = "terms"
        const val LICENSES = "licenses"
        const val NOTICES = "notices"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = intent.getStringExtra(EXTRA_PAGE) ?: PRIVACY
        title = pageTitle(page)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(32))
            setBackgroundColor(getColor(R.color.phnx_cream))
        }
        content.addView(text(pageTitle(page), 28f, true))
        content.addView(text("PHNX Browser\nLast updated: September 5, 2026", 14f, false))
        when (page) {
            TERMS -> addTerms(content)
            LICENSES -> addLicenses(content)
            NOTICES -> addNotices(content)
            else -> addPrivacy(content)
        }

        setContentView(ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.phnx_cream))
            addView(content)
        })
    }

    private fun addPrivacy(parent: LinearLayout) {
        section(parent, "Overview", "PHNX Browser is designed to keep ordinary browsing data on your device and to give you control over profiles, permissions, history, cookies, site storage, and network settings. This page describes the current Android WebView implementation.")
        section(parent, "Data stored on your device", "PHNX stores profile settings, tabs, sessions, bookmarks, history, download records, site permission decisions, privacy choices, identity settings, and network configuration in local app storage. Cookies, cache, Local Storage, IndexedDB, and other supported site data are managed by Android WebView for the active profile.")
        section(parent, "Information sent to other services", "PHNX does not operate a Phoenix analytics or advertising service in this build. Browsing a website sends information to that website. The update checker contacts GitHub to read the latest public release. Free proxy mode contacts HProxy, ProxyScrape, Geonode, Proxifly, and IPLocate for public proxy candidates. These are third-party services, and a public proxy may inspect, modify, or record traffic, so never use one for passwords, payments, or private information.")
        section(parent, "Permissions", "Camera, microphone, and location access are requested only when a site asks for them and Android grants the request. Network access is required for browsing and update checks. PHNX does not request contacts, SMS, phone, or device administrator access.")
        section(parent, "Profiles and private browsing", "Profiles keep supported browser data and app configuration separate. Private tabs are excluded from saved history and session restoration. Android WebView does not provide a fully separate cookie partition for every private tab, so PHNX does not claim complete private-storage isolation beyond the supported cleanup behavior.")
        section(parent, "Your choices", "You can clear selected browsing data, clear history, remove bookmarks, reset site permissions, disable JavaScript or third-party cookies, disable proxy routing, and delete profiles from the app. Android system settings control app permissions and storage outside PHNX.")
        section(parent, "Limitations", "PHNX is not an anonymity service, VPN, security guarantee, or complete tracker-blocking engine. Android WebView controls the browser engine and does not expose every desktop Chromium privacy or device-identity control.")
        section(parent, "Support", "For project questions or privacy concerns, use the Phoenix Browser repository issue tracker: https://github.com/Phoenix1185/phnx-browser/issues")
    }

    private fun addTerms(parent: LinearLayout) {
        section(parent, "Acceptable use", "Use PHNX Browser only in compliance with applicable law, website rules, and the rights of other people. Do not use it to access systems without authorization, distribute malware, evade lawful controls, or abuse websites and network services.")
        section(parent, "No warranty", "PHNX is provided on an as-is and as-available basis. Browsing, downloads, proxy routes, privacy settings, updates, and website compatibility can fail or change. Phoenix does not guarantee availability, security, anonymity, uninterrupted service, or that a website will behave correctly in WebView.")
        section(parent, "Network and proxy use", "You are responsible for any proxy or network route you configure. Free public proxies are operated by unknown third parties and are not suitable for credentials, payment details, confidential work, or sensitive browsing. PHNX does not endorse or control those services.")
        section(parent, "Third-party content", "Websites, downloads, search providers, Android WebView, GitHub, HProxy, and other third-party services have their own terms and privacy practices. You are responsible for reviewing and following the terms that apply to the services you use.")
        section(parent, "Updates and changes", "PHNX may check public release metadata and provide links to official releases. Updates are not silently installed by this build. Features, documentation, and these terms may change as the project develops; the current in-app page applies to the installed build.")
        section(parent, "Open source", "PHNX includes and relies on open-source software. The applicable third-party license terms are listed in Open Source Licenses and Third-Party Notices. Those licenses continue to apply to their respective components.")
        section(parent, "Contact", "Project support is available through the repository issue tracker: https://github.com/Phoenix1185/phnx-browser/issues")
    }

    private fun addLicenses(parent: LinearLayout) {
        section(parent, "AndroidX and Jetpack", "AndroidX Core, Activity, AppCompat, Room, WebKit, and SwipeRefreshLayout are distributed under the Apache License 2.0. https://www.apache.org/licenses/LICENSE-2.0")
        section(parent, "Kotlin", "Kotlin and its standard library are distributed under the Apache License 2.0. https://kotlinlang.org/docs/faq.html#is-kotlin-free")
        section(parent, "Android WebView and Chromium", "The browser engine is supplied by the Android System WebView installed on the device. Chromium and Android components carry their own open-source notices and license terms. https://source.chromium.org/chromium/chromium/src/+/main:LICENSE")
        section(parent, "JUnit", "JUnit is used for project tests under the Eclipse Public License 1.0. https://junit.org/junit4/license.html")
        section(parent, "Complete source licenses", "This screen is a concise notice for the current dependency set. Distribution packages should include the complete license texts and notices for the exact dependency versions used by the build.")
    }

    private fun addNotices(parent: LinearLayout) {
        section(parent, "Android and Chromium", "PHNX uses Android APIs and the Android System WebView. Android, Google, Chromium, and related names and logos are trademarks of their respective owners. PHNX is not an official Google or Chromium product.")
        section(parent, "AndroidX", "The application uses AndroidX libraries for activity, UI, WebView proxy controls, persistence, and testing. AndroidX is provided by the Android Open Source Project under its applicable license terms.")
        section(parent, "Public proxy feeds", "Free proxy mode reads public endpoint data from HProxy, ProxyScrape, Geonode, Proxifly, and IPLocate. These are third-party services and are not controlled or operated by PHNX. Endpoint availability, ownership, privacy, and safety are not guaranteed.")
        section(parent, "GitHub", "The update checker reads public release metadata from GitHub. GitHub is a third-party service and its name and marks belong to GitHub, Inc.")
        section(parent, "No bundled proxy or Chromium server", "PHNX does not bundle a VPN server, proxy server, or separate Chromium distribution. The active Android System WebView supplies the browser engine, and proxy override applies to the app process rather than the whole device.")
    }

    private fun section(parent: LinearLayout, heading: String, body: String) {
        parent.addView(text(heading, 18f, true).apply {
            setPadding(0, dp(24), 0, dp(4))
        })
        parent.addView(text(body, 15f, false))
    }

    private fun text(value: String, size: Float, prominent: Boolean): TextView = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(if (prominent) getColor(R.color.phnx_blue) else getColor(R.color.phnx_text))
        setPadding(0, if (prominent) 0 else dp(8), 0, 0)
        if (!prominent && Linkify.addLinks(this, Linkify.WEB_URLS)) {
            linksClickable = true
            movementMethod = LinkMovementMethod.getInstance()
        }
        setTextIsSelectable(true)
    }

    private fun pageTitle(page: String): String = when (page) {
        TERMS -> "Terms of Use"
        LICENSES -> "Open Source Licenses"
        NOTICES -> "Third-Party Notices"
        else -> "Privacy Policy"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
