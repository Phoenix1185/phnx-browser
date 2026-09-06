# PHNX Browser — Phase 13 Blueprint

## Official Website and GitHub Pages Deployment

> **Scope:** Build and deploy the official PHNX Browser website through GitHub Pages using the project's repository, GitHub Actions, and the default GitHub Pages domain. The site must be prepared for a future custom domain without requiring an architectural rebuild.

> **Brand:** **PHNX Browser** — *Browse Freer.* Built & engineered by Phoenix. © 2026 Phoenix.

## Website architecture

```text
PHNX Browser repository → GitHub Actions → GitHub Pages → <username>.github.io
```

Use a clean, modern, premium browser-product design with a mobile-first layout. The implementation may improve the visual system, animation, typography, components, navigation, and interactions while preserving PHNX identity and functional accuracy.

Do not configure a custom domain yet. Keep the public base URL in one configuration location so a future `phnxbrowser.com` migration changes configuration rather than scattered links.

## Information architecture

The website should be a structured site rather than one unorganized page:

```text
/
├── Home
├── Features
├── Download
├── Releases
├── Changelog
├── Privacy
├── Security
├── Documentation
├── About
└── GitHub
```

Documentation should remain easy to navigate and cover Getting Started, Profiles, Network & Proxy, Privacy & Security, Ad Blocking, Performance, Updates, Default Browser, Permissions, Troubleshooting, and Developer Documentation.

## Homepage and visual identity

The homepage communicates immediately what PHNX Browser is:

```text
PHNX Browser
Browse Freer.
A fast, private and powerful Chromium-based browser built for modern browsing.

[ Download PHNX ] [ Explore Features ]
```

Include the official PHNX app icon/logo prominently and use a browser or device mockup to represent the product. Maintain recognizable PHNX visual identity across the website, favicon, download page, documentation, GitHub repository, and release pages.

Use the official asset at [`assets/branding/app-icon/phnx-browser-app-icon.png`](../../../assets/branding/app-icon/phnx-browser-app-icon.png) as the visual reference. Preserve the phoenix, globe, dark rounded-square treatment, orange/yellow highlights, and PHNX Browser wordmark when creating favicon, social-preview, and adaptive-icon derivatives.

## Features to present

| Capability | Website message |
|---|---|
| Chromium browsing | Fast modern web browsing powered by Chromium. |
| Isolated profiles | Separate cookies, storage, permissions, history, sessions, and network configuration. |
| Network and proxy | Per-profile Direct, HTTP, HTTPS, SOCKS4, SOCKS5, and user-provided proxy behavior where supported. |
| Privacy and security | Privacy controls, permission management, HTTPS, verification, and safe release practices. |
| Ads and tracker blocking | Profile-aware controls, site exceptions, and accurate blocked-request statistics. |
| Performance | Resource management for multiple profiles and browser sessions. |
| Profile lifecycle | Profiles move through active, idle, frozen, and suspended states and can be recreated as needed. |
| Self-updates | Secure release discovery, verification, patching, rollback, and recovery. |
| Browser features | Downloads, bookmarks, history, private tabs, JavaScript, permissions, and default-browser integration. |

Do not make unsupported claims such as complete anonymity, impossible tracking, or total invisibility online.

## Download, releases, and changelog

Provide a dedicated Download page with the latest stable release, version, build, channel, APK link, release link, Android requirement, and SHA-256 checksum. Prefer generating these values dynamically from actual GitHub release metadata. Do not show a download or release that cannot actually be found.

The Releases page links directly to GitHub Releases where applicable, with version, channel, release date, APK, and release notes. The Changelog presents Added, Improved, and Fixed sections and may be generated from release information when practical.

## Privacy and security pages

The Privacy page accurately explains browser data, profiles, cookies, permissions, network configuration, proxy use, ad/tracker blocking, updates, website requests, and crash/error information where applicable. It must describe both protections and limitations.

The Security page explains secure updates, profile isolation, permission controls, HTTPS, cryptographic verification, safe releases, and secure signing. Never expose private keys, keystores, passwords, API keys, update-signing keys, GitHub Actions secrets, or private credentials.

## About and GitHub integration

Use the official About copy:

> **PHNX Browser** is a fast, private, and powerful Chromium-based browser built for modern browsing. Built & engineered by Phoenix. PHNX focuses on performance, privacy, profile isolation, resource management, and a clean modern browsing experience. © 2026 Phoenix.

Show version, build, release channel, and actual Chromium version where available. Include Open Source Licenses, Third-Party Notices, Privacy Policy, and Terms of Service where they reflect actual product behavior.

Use the actual repository URL: [Phoenix1185/phnx-browser](https://github.com/Phoenix1185/phnx-browser). Link to GitHub, documentation, issues, releases, and changelog. Do not invent repository, release, or download URLs.

## GitHub Pages deployment

Add a dedicated workflow such as `.github/workflows/deploy-website.yml`:

```text
Push to production branch
        ↓
Checkout repository
        ↓
Install dependencies
        ↓
Build website
        ↓
Validate build
        ↓
Upload Pages artifact
        ↓
Deploy to GitHub Pages
```

Every push to the configured production branch should deploy automatically. GitHub Pages is the only hosting provider required for this phase. The live URL is the appropriate user or project GitHub Pages URL; it must be discovered from actual repository configuration rather than fabricated.

## Responsive design and performance

The site must work on Android, mobile, desktop, and large screens without horizontal scrolling. Use touch-friendly controls and a mobile-first layout.

Keep the website lightweight. Avoid huge JavaScript bundles, heavy animations, autoplay video, unoptimized images, tracking scripts, and excessive dependencies. Optimize HTML, CSS, JavaScript, images, and fonts for slower mobile networks.

## Metadata and SEO

Use configurable metadata:

```text
Title: PHNX Browser — Browse Freer
Description: PHNX Browser is a fast, private and powerful Chromium-based browser built for modern browsing.
```

Include Open Graph metadata, Twitter/X metadata, favicon, app icon, theme color, and a canonical URL configuration that reflects the actual GitHub Pages domain. Do not add a fake custom-domain canonical URL while GitHub Pages is in use.

## Security and repository hygiene

The public website repository must never contain private keys, keystores, passwords, API keys, tokens, update-signing keys, database credentials, or GitHub secrets. Use GitHub Secrets for sensitive values and keep internal infrastructure details out of the public site.

## Acceptance checklist

- [ ] Website builds successfully.
- [ ] Website works on Android and desktop.
- [ ] GitHub Actions deployment works.
- [ ] GitHub Pages is enabled and the actual `.github.io` URL works.
- [ ] PHNX branding, logo, favicon, and app icon are implemented.
- [ ] Homepage, features, download, releases, changelog, documentation, privacy, security, and About sections work.
- [ ] GitHub links point to the actual repository.
- [ ] No fake URLs or unavailable downloads are presented.
- [ ] No secrets are committed.
- [ ] Navigation is complete and mobile layout has no horizontal overflow.
- [ ] Build assets are optimized.
- [ ] Future custom-domain migration is configuration-based.

## Explicit exclusions

Do not configure a custom domain yet, claim Play Store approval, claim complete anonymity or undetectability, expose internal infrastructure, commit secrets, or represent unavailable release artifacts as downloadable. Preserve the product's real capabilities and limitations.

## Final architecture

```text
PHNX Browser
      ↓
GitHub repository
      ↓
GitHub Actions
      ↓
GitHub Pages
      ↓
<username>.github.io
      ├── Download
      ├── Documentation
      ├── Releases
      ├── Privacy and Security
      └── About PHNX
```

## Related diagrams

Editable Mermaid sources are in [`diagrams/`](./diagrams/), and rendered PNGs are in [`diagrams/rendered/`](./diagrams/rendered/).

- [Website architecture](./diagrams/rendered/website-architecture.png)
- [Site map and content](./diagrams/rendered/site-map.png)
- [GitHub Pages deployment](./diagrams/rendered/pages-deployment.png)
- [Responsive performance](./diagrams/rendered/responsive-performance.png)
- [Branding and security](./diagrams/rendered/branding-security.png)
