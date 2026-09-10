# PHNX Browser Website

This is the static GitHub Pages site for PHNX Browser. It uses the repository's official icon and links to the actual Phoenix repository, issues, releases, legal documents, and product documentation. `build.py` generates the direct-refresh route directories in this folder from one configurable base URL.

The Download, Releases, and Changelog pages resolve published GitHub Releases through the public API and link directly to real APK assets only when they exist. Until a signed stable release with an APK asset exists, they show `No signed public release is currently available.` and expose no CI artifacts, debug builds, unsigned packages, or workflow links as product downloads.

GitHub Pages must use `GitHub Actions` as its source. Every push to `main` runs `.github/workflows/deploy-website.yml`, builds the static routes, validates them, writes `sitemap.xml` and `robots.txt`, uploads the `website/` artifact, and deploys the Pages site. Generated links and assets are route-relative so the site also renders correctly from the legacy `/website/` path.

`.github/workflows/website-health.yml` checks the public routes and static assets every 15 minutes with retries. It detects an outage and makes the failed check visible in GitHub Actions; GitHub Pages itself remains the always-on host and does not need a keep-alive process.
