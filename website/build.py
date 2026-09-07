from html import escape
from pathlib import Path
import re


# Change this one value when moving PHNX to a custom domain.
SITE_URL = "https://phoenix1185.github.io/phnx-browser"
REPO_URL = "https://github.com/Phoenix1185/phnx-browser"
RELEASES_URL = f"{REPO_URL}/releases"
ICON_PATH = "assets/phnx-browser-app-icon.png"

ROOT = Path(__file__).resolve().parent

NAV = [
    ("Features", "features"),
    ("Download", "download"),
    ("Releases", "releases"),
    ("Docs", "docs"),
    ("Privacy", "privacy"),
    ("About", "about"),
]

DOCS = [
    ("getting-started", "Getting Started", "Install, launch, and understand the current product boundary.", "phase-01-foundation"),
    ("profiles", "Profiles", "Storage, sessions, identity, and profile-scoped browser state.", "phase-02-profile-system"),
    ("network", "Network & Proxy", "Direct and configured routes, warnings, and profile-aware network behavior.", "phase-03-network-layer"),
    ("privacy-security", "Privacy & Security", "Cookies, permissions, HTTPS, safe browsing, and honest limitations.", "phase-06-privacy-security-permissions"),
    ("ad-blocking", "Ad Blocking", "Local rules, site exceptions, and request statistics.", "phase-10-ads-tracker-blocker"),
    ("performance", "Performance", "Profile lifecycle, resource policy, and thermal-aware behavior.", "phase-08-polish-performance-thermal"),
    ("updates", "Updates", "Signed release workflow, manifest verification, and the limits of the current discovery-only updater.", "phase-11-self-update-patch-system"),
    ("default-browser", "Default Browser", "Android intents, browser role controls, and external links.", "phase-12-default-browser-system-integration"),
    ("permissions", "Permissions", "Android runtime permissions and website permissions kept separate.", "phase-06-privacy-security-permissions"),
    ("troubleshooting", "Troubleshooting", "Practical checks for profiles, network, permissions, and releases.", "phase-01-foundation"),
    ("developer", "Developer Documentation", "Repository layout, build checks, legal source, and contribution paths.", "phase-13-official-website"),
]


def url(path=""):
    clean = path.strip("/")
    return f"__PHNX_RELATIVE_URL__{clean}__"


def absolute_url(path=""):
    clean = path.strip("/")
    if not clean:
        return f"{SITE_URL}/"
    suffix = "" if Path(clean).suffix else "/"
    return f"{SITE_URL}/{clean}{suffix}"


def relative_url(route, path=""):
    clean = path.strip("/")
    depth = len([part for part in route.strip("/").split("/") if part])
    prefix = "../" * depth
    if not clean:
        return prefix or "./"
    suffix = "" if Path(clean).suffix else "/"
    return f"{prefix}{clean}{suffix}"


def resolve_relative_urls(route, content):
    return re.sub(
        r"__PHNX_RELATIVE_URL__(.*?)__",
        lambda match: relative_url(route, match.group(1)),
        content,
    )


def phase_url(folder):
    return f"{REPO_URL}/tree/main/PHASES/phase-01-foundation/{folder}"


def github_file(path):
    return f"{REPO_URL}/blob/main/{path}"


def site_routes():
    return [
        "",
        "features",
        "download",
        "releases",
        "changelog",
        "docs",
        *(f"docs/{slug}" for slug, _, _, _ in DOCS),
        "privacy",
        "security",
        "about",
        "legal",
    ]


def page_shell(route, title, description, body):
    canonical = absolute_url(route)
    current = "docs" if route.startswith("docs") else route.split("/", 1)[0]
    links = []
    for label, target in NAV:
        selected = ' aria-current="page"' if current == target else ""
        links.append(f'<a href="{relative_url(route, target)}"{selected}>{label}</a>')
    nav = "".join(links)
    html = f'''<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="theme-color" content="#071522">
  <meta name="description" content="{escape(description)}">
  <meta property="og:title" content="{escape(title)}">
  <meta property="og:description" content="{escape(description)}">
  <meta property="og:type" content="website">
  <meta property="og:url" content="{canonical}">
  <meta property="og:image" content="{absolute_url(ICON_PATH)}">
  <meta name="twitter:card" content="summary">
  <link rel="canonical" href="{canonical}">
  <link rel="icon" href="{relative_url(route, ICON_PATH)}">
  <link rel="stylesheet" href="{relative_url(route, 'assets/site.css')}">
  <title>{escape(title)} | PHNX Browser</title>
</head>
<body>
  <header class="topbar">
    <nav class="wrap nav">
      <a class="brand" href="{relative_url(route)}"><img src="{url(ICON_PATH)}" alt="PHNX Browser icon"><span>PHNX BROWSER</span></a>
      <button class="menu" type="button" aria-expanded="false" aria-controls="navlinks">Menu</button>
      <div class="navlinks" id="navlinks">{nav}<a href="{REPO_URL}">GitHub</a></div>
    </nav>
  </header>
  {body}
  <footer>
    <div class="wrap footer-row">
      <span>PHNX Browser. Browse Freer. &copy; 2026 Phoenix.</span>
      <span><a href="{REPO_URL}">GitHub</a> · <a href="{RELEASES_URL}">Releases</a> · <a href="{url('privacy')}">Privacy</a> · <a href="{url('security')}">Security</a></span>
    </div>
  </footer>
  <script src="{relative_url(route, 'assets/site.js')}"></script>
</body>
</html>
'''
    return resolve_relative_urls(route, html)


def page_hero(kicker, headline, intro):
    return f'''<section class="page-hero">
  <div class="wrap">
    <div class="eyebrow">{escape(kicker)}</div>
    <h1>{headline}</h1>
    <p>{intro}</p>
  </div>
</section>'''


def card(title, text, status=None):
    status_html = f'<span class="status">{escape(status)}</span>' if status else ""
    return f'<article class="card"><strong>{escape(title)}</strong><p>{text}</p>{status_html}</article>'


def doc_grid():
    items = []
    for slug, title, text, _ in DOCS:
        items.append(f'<a class="doc" href="{url(f"docs/{slug}")}"><strong>{escape(title)}</strong><span>{escape(text)}</span></a>')
    return "".join(items)


def release_panel():
    return '''<div class="release-panel" data-release-panel>
  <h3 data-release-title>Checking official GitHub Releases...</h3>
  <p data-release-copy>The page will only show a download when a published stable release includes a real APK asset.</p>
  <div class="release-meta" data-release-meta>Source: official Phoenix GitHub Releases</div>
  <div class="actions" data-release-actions><a class="button disabled" href="" aria-disabled="true">Waiting for release data</a></div>
</div>'''


def home():
    body = f'''<main>
  <section class="hero">
    <div class="wrap hero-grid">
      <div>
        <div class="eyebrow">Built and engineered by Phoenix</div>
        <h1>Browse<br><span style="color:var(--yellow)">Freer.</span></h1>
        <p>A fast, private, and powerful Chromium-based Android browser built for modern browsing, with isolated profiles, clear controls, and an honest mobile-first experience.</p>
        <div class="actions"><a class="button primary" href="{url('download')}">Download PHNX</a><a class="button ghost" href="{url('features')}">Explore features</a></div>
      </div>
      <div class="device"><div class="device-screen"><div class="browser-bar"><span class="dot"></span><span class="dot" style="background:#ffc857"></span><span class="dot" style="background:#8de1d0"></span><span class="address">Search or enter address</span></div><div class="screen-content"><img src="{url(ICON_PATH)}" alt="PHNX Browser"><div><strong>PHNX Browser</strong><br><small>Profiles. Privacy. Control.</small></div></div></div></div>
    </div>
  </section>
  <section class="light"><div class="wrap"><div class="section-head"><h2>Built around<br>your browsing.</h2><p>PHNX keeps the everyday browser surface simple while putting profiles, permissions, network choices, and resource behavior within reach.</p></div><div class="cards">{card('Modern browsing', 'Android System WebView provides JavaScript, DOM storage, tabs, history, bookmarks, downloads, find-in-page, zoom, and full-screen support.', 'Implemented')}{card('Isolated profiles', 'Separate supported cookies, storage, permissions, history, sessions, identity settings, and network configuration by profile.', 'Implemented')}{card('Privacy controls', 'Profile-scoped JavaScript, cookie, pop-up, Safe Browsing, permission, clear-data, and blocker controls.', 'Implemented')}{card('Resource-aware', 'Profiles can move through active, idle, frozen, suspended, recreating, and closed lifecycle states.', 'Implemented')}{card('Baseline blocking', 'Intercept supported advertising and tracking requests locally, with site exceptions and accurate blocked-request statistics.', 'Implemented')}{card('Network choice', 'Direct, My Proxy, and Free Public Proxy modes are saved per profile with explicit WebView limitations.', 'Implemented')}{card('Secure updates', 'Signed release workflow, manifest verification, checksums, and release discovery are implemented; public installation remains user-reviewed.', 'Workflow ready')}{card('System integration', 'HTTP and HTTPS intents, browser-role controls, permission prompts, file selection, and safe URL validation are implemented where supported.', 'Implemented')}{card('Open source', 'The repository, phase blueprints, legal source, issues, and release history remain publicly inspectable.', 'Implemented')}</div></div></section>
  <section class="dark"><div class="wrap split"><div><div class="eyebrow">Official status</div><h2>Real status.<br>No theater.</h2></div><div class="prose"><p>PHNX is an active Android browser project. The website describes the current implementation and its limits; it does not claim complete anonymity, undetectability, or a public APK that does not exist.</p><div class="actions"><a class="button primary" href="{url('releases')}">See releases</a><a class="button ghost" href="{url('docs')}">Read the docs</a></div></div></div></section>
  <section class="light"><div class="wrap"><div class="section-head"><div><div class="kicker">Documentation</div><h2>See how it<br>works.</h2></div><p>Architecture notes, current behavior, and limitations stay close to the implementation.</p></div><div class="doc-grid">{doc_grid()}</div></div></section>
  <section class="dark"><div class="wrap split"><div><div class="eyebrow">Privacy</div><h2>Protection and limits, clearly stated.</h2></div><div class="prose"><p><strong>Local by design.</strong> PHNX stores browser data in Android app and WebView profile directories. It does not operate an analytics or advertising service in this build.</p><p><strong>Profiles are meaningful, not magic.</strong> Storage, history, bookmarks, permissions, identity settings, and network settings are scoped where Android WebView allows it. Private tabs provide session and history hygiene, not a separate WebView cookie partition.</p><p><strong>Routes are visible.</strong> Public proxies are third-party routes that may inspect or modify traffic. They are not anonymity tools and must not be used for passwords, payments, or confidential data.</p><div class="actions"><a class="button primary" href="{url('privacy')}">Read privacy</a><a class="button ghost" href="{url('security')}">Read security</a></div></div></div></section>
</main>'''
    return page_shell("", "Browse Freer", "PHNX Browser is a fast, private, and powerful Chromium-based Android browser built for modern browsing.", body)


def features():
    body = f'''<main>
  {page_hero('Features', 'Control the browser surface.', 'PHNX is built around practical Android browsing: profiles, permissions, network choices, resource management, and clear safety boundaries.')}
  <section class="light"><div class="wrap"><div class="cards">{card('Chromium browsing', 'Fast modern web browsing powered by the Android System WebView runtime. The installed WebView provider determines the exact Chromium version.', 'Implemented')}{card('Profiles', 'Profiles separate supported cookies, storage, permissions, history, sessions, identity settings, and network configuration.', 'Implemented')}{card('Network and proxy', 'Direct, My Proxy, and Free Public Proxy modes persist per profile where the device runtime supports them.', 'Implemented')}{card('Privacy and security', 'JavaScript, cookies, pop-ups, Safe Browsing, site permissions, HTTPS state, download checks, and clear-data controls.', 'Implemented')}{card('Ad and tracker blocking', 'Profile-aware local blocking with site exceptions and request statistics. Blocking is not a promise that every ad or tracker is stopped.', 'Implemented')}{card('Performance', 'Resource policy coordinates profile and WebView lifecycle states to keep multiple browsing contexts manageable.', 'Implemented')}{card('Browser features', 'Downloads, bookmarks, history, private tabs, find-in-page, zoom, full-screen, file selection, and external-link intents.', 'Implemented')}{card('Self-updates', 'Signed release manifests and verification primitives are implemented. The app currently discovers releases and asks the user to review them.', 'Workflow ready')}{card('Default browser', 'Android browser-role controls and HTTP/HTTPS intents respect Android system choice and user consent.', 'Implemented')}</div></div></section>
  <section class="dark"><div class="wrap split"><div><div class="eyebrow">Boundaries</div><h2>Useful controls,<br>not magic claims.</h2></div><div class="prose"><ul class="list"><li>PHNX does not claim complete anonymity or total protection from tracking.</li><li>Private tabs do not create a separate WebView cookie partition.</li><li>Public proxies are third-party routes and are not suitable for passwords or payments.</li><li>Website permissions remain separate from Android runtime permissions.</li></ul></div></div></section>
</main>'''
    return page_shell("features", "Features", "Explore the browser controls and capabilities currently implemented in PHNX Browser.", body)


def download():
    body = f'''<main>
  {page_hero('Download', 'Get PHNX Browser.', 'The official download page resolves published GitHub Release metadata at runtime. It never exposes CI artifacts, debug builds, unsigned packages, or invented links.')}
  <section class="light"><div class="wrap split"><div><div class="kicker">Android</div><h2>One honest download path.</h2><p class="lede">The signed-release workflow is configured, but no stable public APK is published until a version-matched release tag is built and signed. This page shows only real GitHub Release assets.</p></div>{release_panel()}</div></section>
  <section class="dark"><div class="wrap split"><div><div class="eyebrow">Before installing</div><h2>Know what you are getting.</h2></div><div class="prose"><ul class="list"><li>Android requirement: Android 9 / API 28 or newer for the current build.</li><li>PHNX uses the device Android System WebView provider for Chromium rendering.</li><li>Review Android's install and permission prompts before installing an APK.</li><li>The app validates release metadata but currently opens the official release page for user-reviewed installation.</li></ul><div class="actions"><a class="button primary" href="{url('releases')}">Release status</a><a class="button ghost" href="{url('security')}">Security details</a></div></div></div></section>
</main>'''
    return page_shell("download", "Download", "Download PHNX Browser only from an official published release with a real APK asset.", body)


def releases():
    body = f'''<main>
  {page_hero('Releases', 'Published in public.', 'GitHub Releases are the source of truth for PHNX versions, notes, assets, and dates. This page filters out drafts and prereleases.')}
  <section class="light"><div class="wrap"><div class="section-head"><div><div class="kicker">Release feed</div><h2>Release history.</h2></div><p>The list below is loaded from the official Phoenix repository. A network or API failure falls back to a clear unavailable state.</p></div><div class="release-list" data-release-list><div class="note">Loading official GitHub Releases...</div></div></div></section>
  <section class="dark"><div class="wrap split"><div><div class="eyebrow">No hidden artifacts</div><h2>Stable means published.</h2></div><div class="prose"><p>Build outputs in GitHub Actions are not presented as product downloads. A release must be published with a real asset before PHNX exposes an APK link.</p><div class="actions"><a class="button primary" href="{RELEASES_URL}">Open GitHub Releases</a><a class="button ghost" href="{url('changelog')}">Read changelog</a></div></div></div></section>
</main>'''
    return page_shell("releases", "Releases", "Published PHNX Browser releases, release notes, and real APK assets from GitHub.", body)


def changelog():
    body = f'''<main>
  {page_hero('Changelog', 'What changed.', 'The current implementation status is visible in the repository and release feed. Published release notes will appear here automatically.')}
  <section class="light"><div class="wrap split"><div><div class="kicker">Current baseline</div><h2>Foundation is in place.</h2></div><div class="prose"><h3>Added</h3><ul><li>Chromium-backed Android browsing shell with tabs, history, bookmarks, downloads, find-in-page, zoom, and full-screen support.</li><li>Profile management, profile-scoped browser state, network configuration, privacy controls, ad/tracker blocking, and lifecycle management.</li><li>Android browser intents, default-browser role controls, permissions, update discovery, and legal screens.</li></ul><h3>Improved</h3><ul><li>Device identity profiles and WebView compatibility behavior are applied through the existing browser architecture.</li><li>Network mode changes now have an explicit save/apply path for Direct, My Proxy, and Free Public Proxy.</li><li>Website routes use relative assets and links so the site works from both the Pages root and the legacy `/website/` path.</li></ul><h3>Fixed</h3><ul><li>Current release state no longer implies an APK is available when the official repository has no published stable asset.</li><li>Added Google Services Gradle configuration for release and debug application variants.</li></ul></div></div></section>
  <section class="dark"><div class="wrap"><div class="section-head"><div><div class="eyebrow">Published releases</div><h2>Release notes.</h2></div><p>Automatically sourced from official GitHub Releases.</p></div><div class="release-list" data-release-list><div class="note">Loading official GitHub Releases...</div></div></div></section>
</main>'''
    return page_shell("changelog", "Changelog", "PHNX Browser implementation updates and published release notes.", body)


def docs_index():
    body = f'''<main>
  {page_hero('Documentation', 'Understand the system.', 'The documentation follows the repository phase blueprints and distinguishes implemented behavior from planned production infrastructure.')}
  <section class="dark"><div class="wrap"><div class="doc-grid">{doc_grid()}</div></div></section>
  <section class="light"><div class="wrap split"><div><div class="kicker">Source</div><h2>Documentation stays close to code.</h2></div><div class="prose"><p>Read the implementation audit, phase blueprints, legal source, and build workflow in the public repository. The website is a navigation layer, not a replacement for the engineering record.</p><div class="actions"><a class="button" href="{REPO_URL}">Open repository</a><a class="button" href="{REPO_URL}/issues">Open issues</a></div></div></div></section>
</main>'''
    return page_shell("docs", "Documentation", "PHNX Browser documentation for features, profiles, network behavior, privacy, updates, and Android integration.", body)


DOC_CONTENT = {
    "getting-started": ("Start with the real product", "Install a published stable APK when one exists, launch PHNX, and open Settings to inspect the current profile, privacy, network, performance, permissions, and default-browser controls. If the Download page reports no public release, there is no signed public APK to install yet.", ["The current build targets Android 9 / API 28 or newer.", "The Android System WebView provider supplies the Chromium runtime.", "The repository's Gradle workflow is the source for build verification."]),
    "profiles": ("Profiles are browser state boundaries", "Profiles separate supported cookies, storage, permissions, history, sessions, identity settings, and network configuration. The separation follows Android WebView capabilities; it is not a claim that every browser implementation detail is perfectly isolated.", ["Private tabs provide session and history hygiene, not a separate WebView cookie partition.", "External links use the default profile unless the existing app flow selects another profile.", "Profile lifecycle states allow active, idle, frozen, suspended, recreating, and closed behavior."]),
    "network": ("Choose the route deliberately", "PHNX supports direct connections and configured HTTP, HTTPS, SOCKS4, and SOCKS5 behavior where the device runtime allows it. Public proxy routes are visible choices with explicit limitations.", ["Proxy settings are profile-aware.", "Public proxies may inspect or modify traffic and are not anonymity tools.", "Do not use public proxy routes for passwords, payments, or confidential data."]),
    "privacy-security": ("Protection with boundaries", "Privacy controls cover JavaScript, cookies, third-party cookies, pop-ups, Safe Browsing, Do Not Track, site permissions, clear-data operations, and blocking settings. The Privacy page describes both protections and limits.", ["Website permissions and Android runtime permissions remain separate.", "HTTPS state and certificate handling are surfaced or enforced where supported.", "No analytics or advertising service is operated by this build."]),
    "ad-blocking": ("Local blocking controls", "The baseline blocker intercepts supported advertising and tracking requests locally and records evaluated and blocked request statistics. Site exceptions can be managed per profile.", ["Blocking is not a promise that every ad or tracker is stopped.", "Rules are local to the browser implementation rather than a remote subscription service.", "Exceptions are explicit and reversible from Privacy & Security settings."]),
    "performance": ("Resource-aware browser sessions", "PHNX coordinates profile and WebView lifecycle state so inactive browser contexts can be idled, frozen, suspended, recreated, or closed according to resource policy.", ["The active profile remains usable while other contexts are reconciled.", "Memory pressure and lifecycle callbacks can trigger resource reconciliation.", "Performance claims depend on the device, Android version, WebView provider, and open pages."]),
    "updates": ("Release discovery is not silent installation", "The app reads official release metadata, validates the signed manifest, compares versions, and lets users review the official release page. GitHub Actions can produce signed APK/AAB artifacts and a signed manifest when a version-matched tag is released.", ["No public APK is shown unless a real published release asset exists.", "Private signing keys, keystores, and update secrets remain outside the repository.", "The app does not yet download and apply updates in the background; delta, rollback-on-startup, staged rollout, and policy scheduling remain future release work."]),
    "default-browser": ("Android system integration", "PHNX registers browser-compatible HTTP and HTTPS intents and routes incoming URLs through the existing URL parser. Android controls chooser behavior when PHNX is not the default browser.", ["Users choose the default browser through Android's role/settings UI.", "Incoming URLs are validated before navigation.", "External intent handling does not expose profile credentials, cookies, or private data."]),
    "permissions": ("Two permission layers", "A website request passes through Chromium's website-permission layer, PHNX's permission manager, and Android runtime permission checks where required. The app does not request camera, microphone, location, notification, or media access merely because a site might use it later.", ["Website camera, microphone, and location approval is separate from Android permission approval.", "Denied permissions are handled without crashing the tab.", "Users can revoke decisions through the existing settings flows."]),
    "troubleshooting": ("Check the source of the problem", "Start by checking the active profile, current network route, Android System WebView provider, Android permission state, and the official release page. The website never presents CI artifacts as a recovery download.", ["If a page fails, test a direct connection and another profile before changing browser data.", "If an APK is unavailable, wait for a published release rather than installing an unsigned build.", "Use the repository issues page for reproducible implementation problems."]),
    "developer": ("Build in public", "The repository contains the Android app, phase blueprints, legal source, website generator, Pages workflow, and tests. The static site is generated by a small standard-library Python script and deployed as a GitHub Pages artifact.", ["Run the website generator with `python3 website/build.py`.", "Run Android checks through the repository Gradle wrapper and the configured Android toolchain.", "The generator writes route pages, `sitemap.xml`, and `robots.txt` under `website/`.", "Never commit API tokens, signing keys, keystores, passwords, or private release infrastructure."]),
}


def doc_page(slug):
    title, intro, points = DOC_CONTENT[slug]
    phase = next(folder for item_slug, _, _, folder in DOCS if item_slug == slug)
    bullets = "".join(f"<li>{escape(point)}</li>" for point in points)
    body = f'''<main>
  {page_hero('Documentation', escape(title), escape(intro))}
  <section class="light"><div class="wrap split"><div><div class="kicker">Read next</div><h2>Current behavior.</h2><div class="actions"><a class="button" href="{url('docs')}">All docs</a><a class="button" href="{phase_url(phase)}">Repository blueprint</a></div></div><div class="prose"><h3>Key points</h3><ul>{bullets}</ul></div></div></section>
</main>'''
    return page_shell(f"docs/{slug}", title, intro, body)


def privacy():
    body = f'''<main>
  {page_hero('Privacy', 'Your data, described plainly.', 'PHNX documents the browser data it manages, the controls it provides, and the limits imposed by Android System WebView and third-party network routes.')}
  <section class="light"><div class="wrap prose"><h2>What PHNX stores</h2><p>PHNX stores browser data in Android app storage and WebView profile directories. Depending on the feature and profile, this includes tabs, session state, history, bookmarks, downloads, cookies, site storage, permissions, identity settings, network settings, and blocker settings.</p><h3>Profiles and private tabs</h3><p>Profiles separate supported browser state. Private tabs provide session and history hygiene, but they do not create a separate WebView cookie partition. Users should not treat private tabs as a guarantee against network observation or every form of tracking.</p><h3>Websites and permissions</h3><p>Website camera, microphone, location, notification, download, JavaScript, cookie, pop-up, and sensor requests are handled through the browser permission layer. Android runtime permissions remain a separate user decision.</p><h3>Network and proxies</h3><p>Direct and configured routes are profile-aware where supported. Public proxies are third-party routes that may inspect or modify traffic. They are not anonymity tools and must not be used for passwords, payments, or confidential data.</p><h3>Diagnostics</h3><p>This build does not operate an analytics or advertising service. Error and crash behavior is handled by the Android app and browser runtime; no claim is made that third-party operating-system diagnostics are absent.</p><div class="actions"><a class="button" href="{github_file('legal/privacy_policy.md')}">Source privacy policy</a><a class="button" href="{url('security')}">Security page</a></div></div></section>
</main>'''
    return page_shell("privacy", "Privacy", "PHNX Browser privacy behavior, profile storage, permissions, network routes, and limitations.", body)


def security():
    body = f'''<main>
  {page_hero('Security', 'Safe defaults. Honest boundaries.', 'PHNX exposes security-relevant decisions instead of hiding them behind marketing claims.')}
  <section class="light"><div class="wrap split"><div><div class="kicker">Browser safety</div><h2>What is implemented.</h2></div><div class="prose"><ul class="list"><li>HTTPS state, certificate-error handling, Android Safe Browsing, mixed-content controls, and dangerous-download checks where supported.</li><li>Website permissions remain explicit and separate from Android runtime permissions.</li><li>Incoming HTTP and HTTPS intents are parsed and validated before navigation.</li><li>Release metadata has typed validation, SHA-256 fields, ECDSA verification primitives, guarded transitions, and a secure Android install handoff.</li></ul></div></div></section>
  <section class="dark"><div class="wrap split"><div><div class="eyebrow">Production boundary</div><h2>Workflow ready.<br>Release not published.</h2></div><div class="prose"><p>Protected Android signing and separate update-manifest signing are wired into the tag-based release workflow. A public APK is still absent until a version-matched tag is built with the repository's protected secrets. Delta application, staged rollout, background policy, and automatic installation remain deliberately unclaimed.</p><div class="actions"><a class="button primary" href="{url('releases')}">Check release status</a><a class="button ghost" href="{github_file('PHASE_AUDIT.md')}">Implementation audit</a></div></div></div></section>
</main>'''
    return page_shell("security", "Security", "PHNX Browser security controls, release verification design, permission boundaries, and current limitations.", body)


def about():
    body = f'''<main>
  {page_hero('About PHNX', 'Built in public.', 'PHNX Browser is built and engineered by Phoenix with a focus on performance, privacy, profile isolation, resource management, and control.')}
  <section class="light"><div class="wrap split"><div><div class="kicker">The project</div><h2>Modern Android browsing.</h2></div><div class="prose"><p><strong>PHNX Browser</strong> is a fast, private, and powerful Chromium-based browser built for modern browsing. It uses the device's Android System WebView provider, so the exact Chromium version depends on the installed WebView runtime.</p><p>The project is open in its repository, with licenses, terms, privacy behavior, implementation notes, limitations, issues, and release status kept visible.</p><div class="actions"><a class="button" href="{REPO_URL}">Repository</a><a class="button" href="{REPO_URL}/issues">Issues</a><a class="button" href="{REPO_URL}/commits/main">Changelog source</a></div></div></div></section>
  <section class="dark"><div class="wrap"><div class="section-head"><div><div class="eyebrow">Project links</div><h2>One official source.</h2></div><p>Use these links from the Android app or any browser to reach the maintained project pages.</p></div><div class="cards">{card('Documentation', f'<a href="{url("docs")}">Read the PHNX docs and blueprints.</a>')}{card('Releases', f'<a href="{url("releases")}">Review published release metadata.</a>')}{card('Legal', f'<a href="{url("legal")}">Privacy, terms, licenses, and notices.</a>')}</div></div></section>
</main>'''
    return page_shell("about", "About", "About PHNX Browser, its Android WebView foundation, open repository, and official project links.", body)


def legal():
    body = f'''<main>
  {page_hero('Legal', 'Documents and notices.', 'The repository is the source for the full legal text. This page keeps the official links easy to find without silently rewriting them.')}
  <section class="light"><div class="wrap"><div class="cards">{card('Privacy Policy', f'<a href="{github_file("legal/privacy_policy.md")}">Read the repository privacy policy.</a>')}{card('Terms of Service', f'<a href="{github_file("legal/terms_of_service.md")}">Read the repository terms.</a>')}{card('Open Source Licenses', f'<a href="{github_file("legal/open_source_licenses.md")}">Review open-source license information.</a>')}{card('Third-Party Notices', f'<a href="{github_file("legal/third_party_notices.md")}">Review third-party notices.</a>')}</div></div></section>
</main>'''
    return page_shell("legal", "Legal", "PHNX Browser privacy, terms, open-source license, and third-party notice links.", body)


def write_page(route, content):
    target = ROOT / (Path(route) / "index.html" if route else "index.html")
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


def main():
    write_page("", home())
    write_page("features", features())
    write_page("download", download())
    write_page("releases", releases())
    write_page("changelog", changelog())
    write_page("docs", docs_index())
    for slug, _, _, _ in DOCS:
        write_page(f"docs/{slug}", doc_page(slug))
    write_page("privacy", privacy())
    write_page("security", security())
    write_page("about", about())
    write_page("legal", legal())
    sitemap = "\n".join(
        [
            '<?xml version="1.0" encoding="UTF-8"?>',
            '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">',
            *[f"  <url><loc>{escape(absolute_url(route))}</loc></url>" for route in site_routes()],
            "</urlset>",
            "",
        ],
    )
    (ROOT / "sitemap.xml").write_text(sitemap, encoding="utf-8")
    (ROOT / "robots.txt").write_text(
        f"User-agent: *\nAllow: /\nSitemap: {SITE_URL}/sitemap.xml\n",
        encoding="utf-8",
    )
    (ROOT / ".nojekyll").write_text("", encoding="utf-8")
    print(f"Generated {len(DOCS) + 10} PHNX website routes in {ROOT}")


if __name__ == "__main__":
    main()
